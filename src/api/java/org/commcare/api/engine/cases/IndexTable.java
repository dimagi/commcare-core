package org.commcare.api.engine.cases;

import org.commcare.cases.query.queryset.DualTableSingleMatchModelQuerySet;

import java.util.*;

/**
 * Created by willpride on 2/21/17.
 */
public interface IndexTable {
    int loadIntoIndexTable(HashMap<String, Vector<Integer>> indexCache, String indexName);

    DualTableSingleMatchModelQuerySet bulkReadIndexToCaseIdMatch(String indexName, Collection<Integer> cuedCases);

    LinkedHashSet<Integer> getCasesMatchingValueSet(String indexName, String[] valueSet);

    LinkedHashSet<Integer> getCasesMatchingIndex(String indexName, String value);
}
