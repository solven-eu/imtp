package eu.solven.adhoc.aggregations;

import java.util.Map;

public class StandardTransformationFactory implements ITransformationFactory {

	@Override
	public ITransformation fromKey(String key, Map<String, ?> options) {
		return switch (key) {
		case SumTransformation.KEY: {
			yield new SumTransformation();
		}
		case MaxTransformation.KEY: {
			yield new MaxTransformation();
		}
		case DivideTransformation.KEY: {
			yield new DivideTransformation();
		}
		case ExpressionTransformation.KEY: {
			yield ExpressionTransformation.parse(options);
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + key);
		};
	}

}