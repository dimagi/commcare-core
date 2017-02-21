package org.commcare.cases.query.queryset;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.expr.XPathExpression;

/**
 * Created by ctsims on 2/6/2017.
 */
public interface ModelQuerySetMatcher {

    QuerySetLookup getQueryLookupFromPredicate(XPathExpression expr);
    QuerySetLookup getQuerySetLookup(TreeReference ref);

}
