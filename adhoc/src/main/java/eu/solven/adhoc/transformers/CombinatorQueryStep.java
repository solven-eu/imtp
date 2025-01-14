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
package eu.solven.adhoc.transformers;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import eu.solven.adhoc.aggregations.ICombination;
import eu.solven.adhoc.aggregations.IOperatorsFactory;
import eu.solven.adhoc.dag.AdhocQueryStep;
import eu.solven.adhoc.dag.CoordinatesToValues;
import eu.solven.adhoc.dag.ICoordinatesToValues;
import eu.solven.adhoc.slice.AdhocSliceAsMap;
import eu.solven.adhoc.slice.AdhocSliceAsMapWithStep;
import eu.solven.adhoc.slice.IAdhocSliceWithStep;
import eu.solven.adhoc.storage.AsObjectValueConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class CombinatorQueryStep implements IHasUnderlyingQuerySteps {
	final ICombinator combinator;
	final IOperatorsFactory transformationFactory;
	final AdhocQueryStep step;

	public List<String> getUnderlyingNames() {
		return combinator.getUnderlyingNames();
	}

	@Override
	public List<AdhocQueryStep> getUnderlyingSteps() {
		return getUnderlyingNames().stream().map(underlyingName -> {
			return AdhocQueryStep.edit(step)
					// Change the requested measureName to the underlying measureName
					.measure(ReferencedMeasure.builder().ref(underlyingName).build())
					.build();
		}).toList();
	}

	@Override
	public ICoordinatesToValues produceOutputColumn(List<? extends ICoordinatesToValues> underlyings) {
		if (underlyings.size() != getUnderlyingNames().size()) {
			throw new IllegalArgumentException("underlyingNames.size() != underlyings.size()");
		} else if (underlyings.isEmpty()) {
			return CoordinatesToValues.empty();
		}

		ICoordinatesToValues output = makeCoordinateToValues();

		ICombination tranformation = transformationFactory.makeTransformation(combinator);

		boolean debug = combinator.isDebug() || step.isDebug();
		for (AdhocSliceAsMap rawSlice : UnderlyingQueryStepHelpers.distinctSlices(combinator.isDebug(), underlyings)) {
			AdhocSliceAsMapWithStep slice = AdhocSliceAsMapWithStep.builder().slice(rawSlice).queryStep(step).build();
			onSlice(underlyings, slice, tranformation, debug, output);
		}

		return output;
	}

	protected void onSlice(List<? extends ICoordinatesToValues> underlyings,
			IAdhocSliceWithStep slice,
			ICombination combination,
			boolean debug,
			ICoordinatesToValues output) {
		List<Object> underlyingVs = underlyings.stream().map(storage -> {
			AtomicReference<Object> refV = new AtomicReference<>();
			AsObjectValueConsumer consumer = AsObjectValueConsumer.consumer(refV::set);

			storage.onValue(slice, consumer);

			return refV.get();
		}).collect(Collectors.toList());

		Object value = combination.combine(slice, underlyingVs);

		if (debug) {
			log.info("[DEBUG] Write {} (given {}) in {} for {}", value, underlyingVs, slice, combinator.getName());
		}

		output.put(slice.getAdhocSliceAsMap(), value);
	}

	protected ICoordinatesToValues makeCoordinateToValues() {
		return CoordinatesToValues.builder().build();
	}

}
