/**
 * The MIT License
 * Copyright (c) 2024 Benoit Chatain Lacelle - SOLVEN
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package eu.solven.adhoc.database.duckdb;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;

import eu.solven.adhoc.IAdhocTestConstants;
import eu.solven.adhoc.ITabularView;
import eu.solven.adhoc.MapBasedTabularView;
import eu.solven.adhoc.aggregations.sum.SumAggregator;
import eu.solven.adhoc.dag.AdhocMeasureBag;
import eu.solven.adhoc.dag.AdhocQueryEngine;
import eu.solven.adhoc.dag.AdhocTestHelper;
import eu.solven.adhoc.database.IAdhocDatabaseTranscoder;
import eu.solven.adhoc.database.MapDatabaseTranscoder;
import eu.solven.adhoc.database.sql.AdhocJooqSqlDatabaseWrapper;
import eu.solven.adhoc.database.sql.DSLSupplier;
import eu.solven.adhoc.query.AdhocQuery;
import eu.solven.adhoc.query.DatabaseQuery;
import eu.solven.adhoc.transformers.Aggregator;

/**
 * This test complex transcoding scenarios, like one underlying column being mapped multiple times by different queried
 * columns
 */
public class TestDatabaseQuery_Transcoding implements IAdhocTestConstants {

	static {
		// https://stackoverflow.com/questions/28272284/how-to-disable-jooqs-self-ad-message-in-3-4
		System.setProperty("org.jooq.no-logo", "true");
	}

	String tableName = "someTableName";

	private Connection makeFreshInMemoryDb() {
		try {
			return DriverManager.getConnection("jdbc:duckdb:");
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	Connection dbConn = makeFreshInMemoryDb();

	@Test
	public void testDifferentQueriedSameUnderlying() {
		// Let's say k1 and k2 rely on the single k DB column
		IAdhocDatabaseTranscoder transcoder =
				MapDatabaseTranscoder.builder().queriedToUnderlying("k1", "k").queriedToUnderlying("k2", "k").build();

		AdhocJooqSqlDatabaseWrapper jooqDb = AdhocJooqSqlDatabaseWrapper.builder()
				.dslSupplier(DSLSupplier.fromConnection(() -> dbConn))
				.tableName(tableName)
				.transcoder(transcoder)
				.build();
		DSLContext dsl = jooqDb.makeDsl();

		dsl.createTableIfNotExists(tableName).column("k", SQLDataType.DOUBLE).execute();
		dsl.insertInto(DSL.table(tableName), DSL.field("k")).values(123).execute();

		{
			DatabaseQuery qK1 = DatabaseQuery.builder().aggregators(Set.of(k1Sum)).build();
			List<Map<String, ?>> dbStream = jooqDb.openDbStream(qK1).collect(Collectors.toList());
			Assertions.assertThat(dbStream).hasSize(1).contains(Map.of("k1", BigDecimal.valueOf(0D + 123)));
		}

		{
			DatabaseQuery qK1K2 = DatabaseQuery.builder().aggregators(Set.of(k1Sum, k2Sum)).build();
			List<Map<String, ?>> dbStream = jooqDb.openDbStream(qK1K2).collect(Collectors.toList());
			Assertions.assertThat(dbStream)
					.hasSize(1)
					.contains(Map.of("k1", BigDecimal.valueOf(0D + 123), "k2", BigDecimal.valueOf(0D + 123)));
		}
	}

	// We request both a mapped column and directly the underlying column
	@Test
	public void testOverlap() {
		// Let's say k1 and k2 rely on the single k DB column
		IAdhocDatabaseTranscoder transcoder = MapDatabaseTranscoder.builder().queriedToUnderlying("k1", "k").build();

		AdhocJooqSqlDatabaseWrapper jooqDb = AdhocJooqSqlDatabaseWrapper.builder()
				.dslSupplier(DSLSupplier.fromConnection(() -> dbConn))
				.tableName(tableName)
				.transcoder(transcoder)
				.build();
		DSLContext dsl = jooqDb.makeDsl();

		dsl.createTableIfNotExists(tableName).column("k", SQLDataType.DOUBLE).execute();
		dsl.insertInto(DSL.table(tableName), DSL.field("k")).values(123).execute();

		{
			// There is an aggregator which name is also an underlying column: it does not be reverseTranscoded
			Aggregator kSum = Aggregator.builder().name("k").aggregationKey(SumAggregator.KEY).build();

			// We request both k1 and k, which are the same in DB
			DatabaseQuery qK1 = DatabaseQuery.builder().aggregators(Set.of(k1Sum, kSum)).build();
			List<Map<String, ?>> dbStream = jooqDb.openDbStream(qK1).collect(Collectors.toList());
			Assertions.assertThat(dbStream)
					.hasSize(1)
					.contains(Map.of("k1", BigDecimal.valueOf(0D + 123), "k", BigDecimal.valueOf(0D + 123)));
		}
	}

	@Test
	public void testCycle() {
		// Let's say k1 and k2 rely on the single k DB column
		IAdhocDatabaseTranscoder transcoder = MapDatabaseTranscoder.builder()
				.queriedToUnderlying("k1", "k2")
				.queriedToUnderlying("k2", "k3")
				.queriedToUnderlying("k3", "k4")
				.queriedToUnderlying("k4", "k1")
				.build();

		AdhocJooqSqlDatabaseWrapper jooqDb = AdhocJooqSqlDatabaseWrapper.builder()
				.dslSupplier(DSLSupplier.fromConnection(() -> dbConn))
				.tableName(tableName)
				.transcoder(transcoder)
				.build();
		DSLContext dsl = jooqDb.makeDsl();

		dsl.createTableIfNotExists(tableName)
				.column("k1", SQLDataType.DOUBLE)
				.column("k2", SQLDataType.DOUBLE)
				.column("k3", SQLDataType.DOUBLE)
				.column("k4", SQLDataType.DOUBLE)
				.execute();
		dsl.insertInto(DSL.table(tableName), DSL.field("k1"), DSL.field("k2"), DSL.field("k3"), DSL.field("k4"))
				.values(123, 234, 345, 456)
				.execute();

		{
			DatabaseQuery qK1 = DatabaseQuery.builder().aggregators(Set.of(k1Sum)).build();
			List<Map<String, ?>> dbStream = jooqDb.openDbStream(qK1).collect(Collectors.toList());
			Assertions.assertThat(dbStream).hasSize(1).contains(Map.of("k1", BigDecimal.valueOf(0D + 234)));
		}

		{
			DatabaseQuery qK1K2 = DatabaseQuery.builder().aggregators(Set.of(k1Sum, k2Sum)).build();
			List<Map<String, ?>> dbStream = jooqDb.openDbStream(qK1K2).collect(Collectors.toList());
			Assertions.assertThat(dbStream)
					.hasSize(1)
					.contains(Map.of("k1", BigDecimal.valueOf(0D + 234), "k2", BigDecimal.valueOf(0D + 345)));
		}

		{
			Aggregator k3Sum = Aggregator.builder().name("k3").aggregationKey(SumAggregator.KEY).build();
			Aggregator k4Sum = Aggregator.builder().name("k4").aggregationKey(SumAggregator.KEY).build();

			DatabaseQuery qK1K2 = DatabaseQuery.builder().aggregators(Set.of(k1Sum, k2Sum, k3Sum, k4Sum)).build();
			List<Map<String, ?>> dbStream = jooqDb.openDbStream(qK1K2).collect(Collectors.toList());
			Assertions.assertThat(dbStream)
					.hasSize(1)
					.contains(Map.of("k1",
							BigDecimal.valueOf(0D + 234),
							"k2",
							BigDecimal.valueOf(0D + 345),
							"k3",
							BigDecimal.valueOf(0D + 456),
							"k4",
							BigDecimal.valueOf(0D + 123)));
		}
	}

	@Test
	public void testAdhocQuery() {
		// Let's say k1 and k2 rely on the single k DB column
		IAdhocDatabaseTranscoder transcoder = MapDatabaseTranscoder.builder().queriedToUnderlying("k1", "k").build();

		AdhocJooqSqlDatabaseWrapper jooqDb = AdhocJooqSqlDatabaseWrapper.builder()
				.dslSupplier(DSLSupplier.fromConnection(() -> dbConn))
				.tableName(tableName)
				.transcoder(transcoder)
				.build();
		DSLContext dsl = jooqDb.makeDsl();

		dsl.createTableIfNotExists(tableName).column("k", SQLDataType.DOUBLE).execute();
		dsl.insertInto(DSL.table(tableName), DSL.field("k")).values(123).execute();

		{
			AdhocQuery query = AdhocQuery.builder().measure(k1Sum.getName()).debug(true).build();

			AdhocMeasureBag measureBag = AdhocMeasureBag.builder().build();
			measureBag.addMeasure(k1Sum);

			AdhocQueryEngine aqe =
					AdhocQueryEngine.builder().eventBus(AdhocTestHelper.eventBus()).measureBag(measureBag).build();

			ITabularView result = aqe.execute(query, jooqDb);
			MapBasedTabularView mapBased = MapBasedTabularView.load(result);

			Assertions.assertThat(mapBased.getCoordinatesToValues())
					.hasSize(1)
					.containsEntry(Map.of(), Map.of("k1", 0L + 123));
		}
	}
}
