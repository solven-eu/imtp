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
package eu.solven.adhoc.aggregations.many_to_many;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;

import eu.solven.adhoc.aggregations.IDecomposition;
import eu.solven.adhoc.api.v1.IAdhocFilter;
import eu.solven.adhoc.api.v1.IWhereGroupbyAdhocQuery;
import eu.solven.adhoc.api.v1.filters.IAndFilter;
import eu.solven.adhoc.api.v1.filters.IColumnFilter;
import eu.solven.adhoc.api.v1.filters.IOrFilter;
import eu.solven.adhoc.api.v1.pojo.AndFilter;
import eu.solven.adhoc.api.v1.pojo.ColumnFilter;
import eu.solven.adhoc.api.v1.pojo.OrFilter;
import eu.solven.adhoc.api.v1.pojo.value.IValueMatcher;
import eu.solven.adhoc.dag.AdhocQueryStep;
import eu.solven.adhoc.query.MeasurelessQuery;
import eu.solven.adhoc.query.groupby.GroupByColumns;
import eu.solven.adhoc.query.groupby.IAdhocColumn;
import eu.solven.adhoc.query.groupby.ReferencedColumn;
import eu.solven.adhoc.slice.IAdhocSliceWithStep;
import eu.solven.pepper.mappath.MapPathGet;
import lombok.extern.slf4j.Slf4j;

/**
 * This is an example {@link IDecomposition}, which copy the underlying value for each output associated to the input
 * value.
 * <p>
 * For instance, given v=200 on element=FR, we write v=200 into group=G8 and group=G20
 */
@Slf4j
public class ManyToManyDecomposition implements IDecomposition {
	public static final String KEY = "many_to_many";
	/**
	 * The column used as elements: the underlying measure is expressed on this column
	 */
	public static final String K_INPUT = "element";
	/**
	 * The column written by this decomposition.
	 */
	public static final String K_OUTPUT = "group";

	final Map<String, ?> options;

	final IManyToManyDefinition manyToManyDefinition;

	public ManyToManyDecomposition(Map<String, ?> options) {
		this(options, new ManyToManyInMemoryDefinition());

		log.warn("Instantiated with default/empty {}", this.manyToManyDefinition);
	}

	public ManyToManyDecomposition(Map<String, ?> options, IManyToManyDefinition manyToManyDefinition) {
		this.options = options;
		this.manyToManyDefinition = manyToManyDefinition;

		String elementColumn = MapPathGet.getRequiredString(options, K_INPUT);
		String groupColumn = MapPathGet.getRequiredString(options, K_OUTPUT);

		if (elementColumn.equals(groupColumn)) {
			throw new UnsupportedOperationException("TODO This case requires specific behaviors and unitTests");
		}
	}

	@Override
	public Map<Map<String, ?>, Object> decompose(IAdhocSliceWithStep slice, Object value) {
		String elementColumn = MapPathGet.getRequiredString(options, K_INPUT);

		Optional<?> optInput = slice.optFilter(elementColumn);
		if (optInput.isEmpty()) {
			// There is no expressed element
			return Map.of(Map.of(), value);
		}

		Object element = optInput.get();

		if (element instanceof Collection<?>) {
			throw new UnsupportedOperationException("TODO Handle element being a Collection");
		}

		Set<Object> groups = getGroups(slice, element);

		String groupColumn = MapPathGet.getRequiredString(options, K_OUTPUT);

		return makeDecomposition(element, value, groupColumn, groups);
	}

	private Map<Map<String, ?>, Object> makeDecomposition(Object element,
			Object value,
			String groupColumn,
			Set<Object> groups) {
		Map<Map<String, ?>, Object> output = new HashMap<>();

		groups.forEach(group -> {
			output.put(Map.of(groupColumn, group), scale(element, value));
		});

		return output;
	}

	protected Set<Object> getGroups(IAdhocSliceWithStep slice, Object element) {
		Set<Object> groupsMayBeFilteredOut = manyToManyDefinition.getGroups(element);

		IAdhocFilter filter = slice.getQueryStep().getFilter();

		Set<String> queryMatchingGroups = getQueryMatchingGroups(slice, filter);

		Set<Object> matchingGroups;

		// Sets.intersection will iterate over the first Set: we observe it is faster the consider the smaller Set first
		if (groupsMayBeFilteredOut.size() < queryMatchingGroups.size()) {
			matchingGroups = Sets.intersection(groupsMayBeFilteredOut, queryMatchingGroups);
		} else {
			matchingGroups =
					Sets.intersection(Collections.unmodifiableSet(queryMatchingGroups), groupsMayBeFilteredOut);
		}

		log.debug("Element={} led to accepted groups={}", element, matchingGroups);

		return matchingGroups;
	}

	private Set<String> getQueryMatchingGroups(IAdhocSliceWithStep slice, IAdhocFilter filter) {
		Map<Object, Object> queryStepCache = slice.getQueryStep().getCache();

		// The groups valid given the filter: we compute it only once as an element may matches many groups: we do not
		// want to filter all groups for each element
		Set<String> queryMatchingGroups = (Set<String>) queryStepCache.computeIfAbsent("matchingGroups", cacheKey -> {
			return getQueryMatchingGroupsNoCache(filter);
		});
		return queryMatchingGroups;
	}

	private Set<?> getQueryMatchingGroupsNoCache(IAdhocFilter filter) {
		String groupColumn = MapPathGet.getRequiredString(options, K_OUTPUT);

		return manyToManyDefinition.getMatchingGroups(group -> doFilter(groupColumn, group, filter));
	}

	protected boolean doFilter(String groupColumn, Object groupCandidate, IAdhocFilter filter) {
		if (filter.isMatchAll()) {
			return true;
		} else if (filter.isColumnFilter() && filter instanceof IColumnFilter columnFilter) {
			if (!columnFilter.getColumn().equals(groupColumn)) {
				// The group column is not filtered: accept this group as it is not rejected
				// e.g. we filter color=pink: it should not reject countryGroup=G8
				return true;
			} else {
				boolean match = columnFilter.getValueMatcher().match(groupCandidate);

				log.debug("{} is matched", groupCandidate);

				return match;
			}
		} else if (filter.isAnd() && filter instanceof IAndFilter andFilter) {
			return andFilter.getOperands()
					.stream()
					.allMatch(subFilter -> doFilter(groupColumn, groupCandidate, subFilter));
		} else if (filter.isOr() && filter instanceof IOrFilter orFilter) {
			return orFilter.getOperands()
					.stream()
					.anyMatch(subFilter -> doFilter(groupColumn, groupCandidate, subFilter));
		} else {
			throw new UnsupportedOperationException("%s is not managed".formatted(filter));
		}
	}

	/**
	 * @param input
	 * @param value
	 * @return the value to attach to given group.
	 */
	protected Object scale(Object input, Object value) {
		// By default, we duplicate the value for each group
		return value;
	}

	@Override
	public List<IWhereGroupbyAdhocQuery> getUnderlyingSteps(AdhocQueryStep step) {
		String elementColumn = MapPathGet.getRequiredString(options, K_INPUT);
		String groupColumn = MapPathGet.getRequiredString(options, K_OUTPUT);

		IAdhocFilter requestedFilter = step.getFilter();
		IAdhocFilter underlyingFilter = convertGroupsToElementsFilter(groupColumn, elementColumn, requestedFilter);

		if (!step.getGroupBy().getGroupedByColumns().contains(groupColumn)) {
			// None of the requested column is an output column of this decomposition : there is nothing to decompose
			return Collections.singletonList(MeasurelessQuery.builder()
					.filter(underlyingFilter)
					.groupBy(step.getGroupBy())
					.customMarker(step.getCustomMarker())
					.build());
		}

		// If we are requested on the dispatched level, we have to groupBy the input level
		Set<IAdhocColumn> allGroupBys = new HashSet<>();
		allGroupBys.addAll(step.getGroupBy().getNameToColumn().values());
		// The groupColumn is generally meaningless to the underlying measure
		allGroupBys.removeIf(c -> c.getColumn().equals(groupColumn));

		String inputColumn = MapPathGet.getRequiredString(options, K_INPUT);
		allGroupBys.add(ReferencedColumn.ref(inputColumn));

		// TODO If we filter some group, we should propagate as filtering some element
		// step.getFilter().

		return Collections.singletonList(MeasurelessQuery.builder()
				.filter(underlyingFilter)
				.groupBy(GroupByColumns.of(allGroupBys))
				.customMarker(step.getCustomMarker())
				.build());
	}

	protected IAdhocFilter convertGroupsToElementsFilter(String groupColumn,
			String elementColumn,
			IAdhocFilter requestedFilter) {
		IAdhocFilter underlyingFilter;

		if (requestedFilter.isMatchAll()) {
			underlyingFilter = requestedFilter;
		} else if (requestedFilter.isColumnFilter() && requestedFilter instanceof IColumnFilter columnFilter) {
			// If it is the group column which is filtered, convert it into an element filter
			if (columnFilter.getColumn().equals(groupColumn)) {
				// Plain filter on the group column: transform it into a filter into the input column
				Set<?> elements = elementsMatchingGroups(columnFilter.getValueMatcher());
				IAdhocFilter elementAdditionalFilter = ColumnFilter.isIn(elementColumn, elements);

				underlyingFilter = elementAdditionalFilter;
			} else {
				underlyingFilter = requestedFilter;
			}
		} else if (requestedFilter.isAnd() && requestedFilter instanceof IAndFilter andFilter) {
			List<IAdhocFilter> elementsFilters = andFilter.getOperands()
					.stream()
					.map(a -> convertGroupsToElementsFilter(groupColumn, elementColumn, a))
					.toList();

			underlyingFilter = AndFilter.and(elementsFilters);
		} else if (requestedFilter.isOr() && requestedFilter instanceof IOrFilter orFilter) {
			List<IAdhocFilter> elementsFilters = orFilter.getOperands()
					.stream()
					.map(a -> convertGroupsToElementsFilter(groupColumn, elementColumn, a))
					.toList();

			underlyingFilter = OrFilter.or(elementsFilters);
		} else {
			throw new UnsupportedOperationException("TODO handle requestedFilter=%s".formatted(requestedFilter));
		}

		return underlyingFilter;
	}

	protected Set<?> elementsMatchingGroups(IValueMatcher valueMatcher) {
		return manyToManyDefinition.getElementsMatchingGroups(valueMatcher);
	}

}
