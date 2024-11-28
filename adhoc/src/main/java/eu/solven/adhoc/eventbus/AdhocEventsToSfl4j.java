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
package eu.solven.adhoc.eventbus;

import org.greenrobot.eventbus.Subscribe;

import lombok.extern.slf4j.Slf4j;

/**
 * This logs main steps of the query-engine. It is typically activated by calling `#AdhocQueryBuilder.debug()`.
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public class AdhocEventsToSfl4j {
	@Subscribe
	public void onMeasuratorIsCompleted(MeasuratorIsCompleted event) {
		log.info("size={} for measure={} on completed (source={})",
				event.getNbCells(),
				event.getMeasure(),
				event.getSource());
	}

	@Subscribe
	public void onAdhocQueryPhaseIsCompleted(AdhocQueryPhaseIsCompleted event) {
		log.info("query phase={} is completed (source={})", event.getPhase(), event.getSource());
	}

	@Subscribe
	public void onQueryStepIsEvaluating(QueryStepIsEvaluating event) {
		log.info("queryStep={} is evaluating (source={})", event.getQueryStep(), event.getSource());
	}

}
