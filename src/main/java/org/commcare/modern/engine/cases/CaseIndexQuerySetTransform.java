package org.commcare.modern.engine.cases;

import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.queryset.*;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.EvaluationTrace;

import java.util.Set;

/**
 * Created by ctsims on 2/6/2017.
 */

public class CaseIndexQuerySetTransform implements QuerySetTransform {

    private final IndexTable table;

    public CaseIndexQuerySetTransform(IndexTable table) {
        this.table = table;
    }

    @Override
    public QuerySetLookup getTransformedLookup(QuerySetLookup incoming, TreeReference relativeLookup) {
        if(incoming.getQueryModelId().equals(CaseQuerySetLookup.CASE_MODEL_ID)) {
            if(relativeLookup.size() == 2 && "index".equals(relativeLookup.getName(0))) {
                String indexName = relativeLookup.getName(1);
                return new CaseIndexQuerySetLookup(indexName, incoming, table);
            }
        }
        return null;
    }



    public static class CaseIndexQuerySetLookup extends DerivedCaseQueryLookup {
        String indexName;
        IndexTable table;

        public CaseIndexQuerySetLookup(String indexName, QuerySetLookup incoming, IndexTable table) {
            super(incoming);
            this.indexName = indexName;
            this.table = table;
        }

        @Override
        protected ModelQuerySet loadModelQuerySet(QueryContext queryContext) {
            EvaluationTrace trace = new EvaluationTrace("Load Query Set Transform[" +
                    getRootLookup().getCurrentQuerySetId() + "]=>[" +
                    this.getCurrentQuerySetId()+ "]");


            Set<Integer> querySetBody = getRootLookup().getLookupSetBody(queryContext);
            DualTableSingleMatchModelQuerySet ret = table.bulkReadIndexToCaseIdMatch(indexName, querySetBody);
            cacheCaseModelQuerySet(queryContext, ret);

            trace.setOutcome("Loaded: " + ret.getSetBody().size());

            queryContext.reportTrace(trace);
            return ret;
        }

        private void cacheCaseModelQuerySet(QueryContext queryContext, DualTableSingleMatchModelQuerySet ret) {
            if(ret.getSetBody().size() > 50 && ret.getSetBody().size() < CaseGroupResultCache.MAX_PREFETCH_CASE_BLOCK) {
                queryContext.getQueryCache(CaseGroupResultCache.class).reportBulkCaseBody(this.getCurrentQuerySetId(), ret.getSetBody());
            }
        }

        @Override
        public String getQueryModelId() {
            return getRootLookup().getQueryModelId();
        }

        @Override
        public String getCurrentQuerySetId() {
            return getRootLookup().getCurrentQuerySetId() + "|index|"+indexName;
        }
    }
}
