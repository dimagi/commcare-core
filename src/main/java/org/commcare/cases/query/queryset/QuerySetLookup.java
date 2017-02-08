package org.commcare.cases.query.queryset;

import org.commcare.cases.query.QueryContext;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.List;
import java.util.Set;

/**
 * Note: Query Set Lookup objects should not persist any state!
 *
 * Created by ctsims on 2/6/2017.
 */

public interface QuerySetLookup {
    boolean  isValid(TreeReference ref, QueryContext context);

    String getQueryModelId();

    String getCurrentQuerySetId();

    TreeReference getLookupIdKey(EvaluationContext evaluationContext);

    List<Integer> performSetLookup(TreeReference lookupIdKey, QueryContext queryContext);

    Set<Integer> getLookupSetBody(QueryContext queryContext);
}
