package eu.solven.adhoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.greenrobot.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.solven.adhoc.aggregations.SumAggregator;
import eu.solven.adhoc.aggregations.SumTransformation;
import eu.solven.adhoc.dag.DAG;
import eu.solven.adhoc.eventbus.AdhocEventsToSfl4j;
import eu.solven.adhoc.query.StandardQueryOptions;
import eu.solven.adhoc.transformers.Aggregator;
import eu.solven.adhoc.transformers.Combinator;

public class TestQueryOptions {
	EventBus eventBus = new EventBus();
	AdhocEventsToSfl4j toSlf4j = new AdhocEventsToSfl4j();
	DAG dag = new DAG(eventBus);

	@BeforeEach
	public void wireEvents() {
		eventBus.register(toSlf4j);
	}

	@Test
	public void testSumOfSum_() {
		dag.addMeasure(Combinator.builder()
				.name("sumK1K2")
				.underlyingMeasurators(Arrays.asList("k1", "k2"))
				.transformationKey(SumTransformation.KEY)
				.build());

		dag.addMeasure(Aggregator.builder().name("k1").aggregationKey(SumAggregator.KEY).build());
		dag.addMeasure(Aggregator.builder().name("k2").aggregationKey(SumAggregator.KEY).build());

		List<Map<String, ?>> rows = new ArrayList<>();

		rows.add(Map.of("k1", 123));
		rows.add(Map.of("k2", 234));
		rows.add(Map.of("k1", 345, "k2", 456));

		ITabularView output =
				dag.execute(Set.of("sumK1K2"), Set.of(StandardQueryOptions.RETURN_UNDERLYING_MEASURES), rows.stream());

		List<Map<String, ?>> keySet = output.keySet().collect(Collectors.toList());
		Assertions.assertThat(keySet).hasSize(1).contains(Collections.emptyMap());

		MapBasedTabularView mapBased = new MapBasedTabularView();

		output.acceptScanner((coordinates, values) -> {
			mapBased.coordinatesToValues.put(coordinates, values);
		});

		Assertions.assertThat(mapBased.coordinatesToValues)
				.hasSize(1)
				.containsEntry(Collections.emptyMap(),
						Map.of("k1", 0L + 123 + 345, "k2", 0L + 234 + 456, "sumK1K2", 0L + 123 + 234 + 345 + 456));
	}
}