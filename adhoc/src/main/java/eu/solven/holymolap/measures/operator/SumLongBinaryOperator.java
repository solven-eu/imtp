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
package eu.solven.holymolap.measures.operator;

import eu.solven.holymolap.stable.v1.IDoubleBinaryOperator;
import eu.solven.holymolap.stable.v1.ILongBinaryOperator;

/**
 * Sum over {@link Long}
 * 
 * @author Benoit Lacelle
 *
 */
public class SumLongBinaryOperator implements ILongBinaryOperator, IDoubleBinaryOperator {

	@Override
	public long applyAsLong(long left, long right) {
		return left + right;
	}

	@Override
	public long neutralAsLong() {
		return 0L;
	}

	@Override
	public double applyAsDouble(double left, double right) {
		return left + right;
	}

	@Override
	public double neutralAsDouble() {
		return 0D;
	}

	@Override
	public Object apply(Object left, Object right) {
		if (left instanceof Integer && right instanceof Integer) {
			return ILongBinaryOperator.super.apply(left, right);
		} else if (left instanceof Long && right instanceof Long) {
			return ILongBinaryOperator.super.apply(left, right);
		} else {
			// Default to double behavior
			return IDoubleBinaryOperator.super.apply(left, right);
		}
	}

	@Override
	public Object neutral() {
		return neutralAsLong();
	}

}