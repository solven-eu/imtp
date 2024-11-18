package eu.solven.adhoc.query;

import java.util.Collections;
import java.util.Set;

import eu.solven.adhoc.api.v1.IAdhocFilter;
import eu.solven.adhoc.api.v1.IAdhocGroupBy;
import eu.solven.adhoc.api.v1.IHasFilters;
import eu.solven.adhoc.api.v1.IHasGroupBy;
import eu.solven.adhoc.api.v1.IWhereGroupbyAdhocQuery;
import eu.solven.adhoc.transformers.Aggregator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

/**
 * A Database query is dedicated to querying external database.
 * 
 * @author Benoit Lacelle
 *
 */
@Value
@AllArgsConstructor
@Builder
public class DatabaseQuery implements IWhereGroupbyAdhocQuery {

	@Default
	protected IAdhocFilter filter = IAdhocFilter.MATCH_ALL;

	@Default
	protected IAdhocGroupBy groupBy = IAdhocGroupBy.GRAND_TOTAL;
	// We query only simple aggregations to external databases
	@Default
	protected Set<Aggregator> aggregators = Collections.emptySet();

	@Default
	boolean debug = false;
	@Default
	boolean explain = false;

	public DatabaseQuery(IHasFilters hasFilter, IHasGroupBy groupBy, Set<Aggregator> aggregators) {
		this.filter = hasFilter.getFilter();
		this.groupBy = groupBy.getGroupBy();
		this.aggregators = aggregators;

		this.debug = false;
		this.explain = false;
	}

	@Override
	public IAdhocFilter getFilter() {
		return filter;
	}

	@Override
	public IAdhocGroupBy getGroupBy() {
		return groupBy;
	}

	public Set<Aggregator> getAggregators() {
		return aggregators;
	}

	public static DatabaseQueryBuilder edit(DatabaseQuery dq) {
		return DatabaseQuery.edit((IWhereGroupbyAdhocQuery) dq)
				.aggregators(dq.getAggregators())
				.debug(dq.isDebug())
				.explain(dq.isExplain());
	}

	public static DatabaseQueryBuilder edit(IWhereGroupbyAdhocQuery dq) {
		return DatabaseQuery.builder().filter(dq.getFilter()).groupBy(dq.getGroupBy());
	}

	// @Override
	// public String toString() {
	// // We call the getters to workaround usage of lambda
	// return "SimpleAggregationQuery [axesFilters=" + axesFilters
	// .getFilter() + ", groupBys=" + axes.getGroupBys() + ", aggregators=" + aggregators + "]";
	// }
}