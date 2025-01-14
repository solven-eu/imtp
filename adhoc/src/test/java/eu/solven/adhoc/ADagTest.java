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
package eu.solven.adhoc;

import org.greenrobot.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;

import eu.solven.adhoc.dag.AdhocMeasureBag;
import eu.solven.adhoc.dag.AdhocQueryEngine;
import eu.solven.adhoc.database.InMemoryDatabase;
import eu.solven.adhoc.eventbus.AdhocEventsToSfl4j;

/**
 * Helps testing anything related with a {@link AdhocMeasureBag} or a {@link AdhocQueryEngine}
 * 
 * @author Benoit Lacelle
 *
 */
public abstract class ADagTest {
	public final EventBus eventBus = new EventBus();
	public final AdhocEventsToSfl4j toSlf4j = new AdhocEventsToSfl4j();
	public final AdhocMeasureBag amb = AdhocMeasureBag.builder().build();
	public final AdhocQueryEngine aqe = AdhocQueryEngine.builder().eventBus(eventBus).measureBag(amb).build();

	public final InMemoryDatabase rows = InMemoryDatabase.builder().build();

	@BeforeEach
	public void wireEvents() {
		eventBus.register(toSlf4j);
	}

	// `@BeforeEach` has to be duplicated on each implementation
	// @BeforeEach
	public abstract void feedDb();

}
