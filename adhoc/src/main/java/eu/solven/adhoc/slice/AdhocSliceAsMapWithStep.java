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
package eu.solven.adhoc.slice;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import eu.solven.adhoc.dag.AdhocQueryStep;
import lombok.Builder;
import lombok.NonNull;

/**
 * A simple {@link IAdhocSlice} based on a {@link Map}
 */
@Builder
public class AdhocSliceAsMapWithStep implements IAdhocSliceWithStep {
	@NonNull
	final IAdhocSlice slice;

	@NonNull
	final AdhocQueryStep queryStep;

	@Override
	public @NonNull AdhocQueryStep getQueryStep() {
		return queryStep;
	}

	@Override
	public Set<String> getColumns() {
		return slice.getColumns();
	}

	@Override
	public Optional<Object> optFilter(String column) {
		return slice.optFilter(column);
	}

	@Override
	public AdhocSliceAsMap getAdhocSliceAsMap() {
		return AdhocSliceAsMap.fromMap(slice.getCoordinates());
	}

}