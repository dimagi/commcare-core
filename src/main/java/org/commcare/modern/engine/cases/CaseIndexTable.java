package org.commcare.modern.engine.cases;

import org.commcare.cases.model.Case;
import org.commcare.cases.query.queryset.DualTableSingleMatchModelQuerySet;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Vector;

/**
 * Created by willpride on 2/21/17.
 */
public interface CaseIndexTable {
    int loadIntoIndexTable(HashMap<String, Vector<Integer>> indexCache, String indexName);

    DualTableSingleMatchModelQuerySet bulkReadIndexToCaseIdMatch(String indexName, Collection<Integer> cuedCases);

    LinkedHashSet<Integer> getCasesMatchingValueSet(String indexName, String[] valueSet);

    LinkedHashSet<Integer> getCasesMatchingIndex(String indexName, String value);

    void indexCase(Case c);

    void clearCaseIndices(Collection<Integer> idsToClear);

    void delete();

    boolean isStorageExists();
}
