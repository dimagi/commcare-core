package org.commcare.cases.query.queryset;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.expr.XPathExpression;

/**
 * A ModelQuerySetMatcher identifies from a provided XPathExpression or TreeReference that the
 * expression/reference pattern is referring to a semantic concept that can be evaluated with
 * a query set lookup, and provides the relevant lookup object which can be used to potentially
 * match and perform that lookup.
 *
 * For Example:
 * in the caseb
 *
 * the predicate:
 * [@case_id = 'hardcoded']
 *
 * could be semantically encoded as a QuerySetLookup which returns a case's internal model id,
 * entirely bypassing the process of performing XPath Evalutions.
 *
 * Similarly, the predicate
 * [index/parent = current()/@case_id]
 *
 * could be interpreted semantically by knowing that current()/@case_id will be an ID that can
 * be matched to a set of cases's index/parent. A matcher could return a lookup which will take
 * in a context and identify the case to be matched and return the model ID of cases that index it
 * without having to have the engine perform that iteratively.
 *
 * //TODO: currently these matchers only have the ability to reason within the current model,
 * but we may need to reach between models to match complex query lookps
 *
 * Created by ctsims on 2/6/2017.
 */
public interface ModelQuerySetMatcher {

    /**
     * Given a predicate expression, identify whether or not it can be expressed as a query set
     * lookup, regardless of the current context.
     */
    QuerySetLookup getQueryLookupFromPredicate(XPathExpression expr);

    /**
     * Given an explicit tree reference, identify a direct query set lookup (if any) that can be
     * used to look up model id's that match that reference.
     *
     * example:
     * instance('casedb')/casedb/case[4]/@case_id
     * and
     * instance('casedb')/casedb/case[4]/index/parent
     *
     * both could produce QuerySetLookup objects with the "case" query model id since they both
     * refer to specific cases in the case model.
     */
    QuerySetLookup getQuerySetLookup(TreeReference ref);

}
