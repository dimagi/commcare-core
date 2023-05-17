package org.commcare.cases.query.queryset;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathEqExpr;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Generates potential model query set lookups for references into the case database model.
 *
 * Chains entity lookups where relevant using model set transforms, which can be added dynamically.
 *
 * example:
 * [@case_id = current()/@case_id]
 *
 * can be directly returned and interpreted as an model query set lookup which gets the current
 * case without needing to compare string Id's, match on looked up values, etc.
 *
 * Created by ctsims on 2/6/2017.
 */

public class CaseModelQuerySetMatcher implements ModelQuerySetMatcher {
    private final Collection<XPathPathExpr> membershipIndexes;

    private TreeReference caseDbRoot;
    private Map<Integer, Integer> multiplicityMap;

    private Vector<QuerySetTransform> querySetTransforms = new Vector<>();
    private Vector<QuerySetTransform> outputSetTransforms = new Vector<>();

    public CaseModelQuerySetMatcher(Map<Integer, Integer> multiplicityMap) {
        this("casedb", multiplicityMap);
    }

    private CaseModelQuerySetMatcher(String modelId,
                                     Map<Integer, Integer> multiplicityMap) {
        caseDbRoot =
                XPathReference.getPathExpr("instance('" + modelId + "')/casedb/case").getReference();

        //Later on we need this to refer to a real element at casedb, not a virtual one
        caseDbRoot.setMultiplicity(0, 0);
        this.multiplicityMap = multiplicityMap;

        membershipIndexes = new Vector<>();
        membershipIndexes.add(CaseInstanceTreeElement.CASE_ID_EXPR);
        membershipIndexes.add(CaseInstanceTreeElement.CASE_ID_EXPR_TWO);
        addQuerySetTransform(new CaseIdentityQuerySetTransform());
        outputSetTransforms.add(new CaseIdentityQuerySetTransform());
    }

    public void addQuerySetTransform(QuerySetTransform transform) {
        this.querySetTransforms.add(transform);
    }

    public void addMatchAndOutputSetTransform(XPathPathExpr match, QuerySetTransform transform) {
        this.membershipIndexes.add(match);
        this.outputSetTransforms.add(transform);
    }

    @Override
    public QuerySetLookup getQueryLookupFromPredicate(XPathExpression expr) {
        if (expr instanceof XPathEqExpr && ((XPathEqExpr)expr).op == XPathEqExpr.EQ) {
            XPathEqExpr eq = ((XPathEqExpr)expr);
            if (!(eq.b instanceof XPathPathExpr)) {
                return null;
            }
            for (XPathPathExpr member : membershipIndexes) {
                if (member.matches(eq.a)) {
                    TreeReference ref = ((XPathPathExpr)eq.b).getReference();
                    return getQuerySetLookup(ref);
                }
            }
        }
        return null;
    }


    @Override
    public QuerySetLookup getQuerySetLookup(TreeReference ref) {
        QuerySetLookup lookup;
        TreeReference remainder;

        if (caseDbRoot.isParentOf(ref, false)) {
            if (!ref.hasPredicates()) {
                return null;
            }

            List<XPathExpression> predicates = ref.getPredicate(caseDbRoot.size() - 1);
            if (predicates == null || predicates.size() > 1) {
                return null;
            }

            lookup = getQueryLookupFromPredicate(predicates.get(0));
            if (lookup == null) {
                return null;
            }
            remainder = ref.getRelativeReferenceAfter(caseDbRoot.size());
        } else if (isCurrentRef(ref)) {
            lookup = new CaseQuerySetLookup(caseDbRoot, multiplicityMap);
            remainder = ref.getRelativeReferenceAfter(0);
        } else {
            return null;
        }

        return getTransformedQuerySetLookup(lookup, remainder);
    }

    private QuerySetLookup getTransformedQuerySetLookup(QuerySetLookup lookup,
                                                        TreeReference remainder) {

        return transformQuerySetLookup(lookup, remainder, querySetTransforms);
    }

    public QuerySetLookup getTransformedQuerySetLookupForOutput(QuerySetLookup lookup,
                                                       TreeReference remainder) {
        return transformQuerySetLookup(lookup, remainder, outputSetTransforms);
    }

    private QuerySetLookup transformQuerySetLookup(
            QuerySetLookup lookup, TreeReference remainder, Vector<QuerySetTransform> transforms) {
        for (QuerySetTransform transform : transforms) {
            QuerySetLookup retVal = transform.getTransformedLookup(lookup, remainder);
            if (retVal != null) {
                return retVal;
            }
        }
        return null;
    }

    private boolean isCurrentRef(TreeReference ref) {
        return ref.getContextType() == TreeReference.CONTEXT_ORIGINAL;
    }


    /**
     * A transform for the situation where the /@case_id step is taken relative to an existing
     * case model query set lookup.
     */
    private static class CaseIdentityQuerySetTransform implements QuerySetTransform {
        static TreeReference caseIdRef = CaseInstanceTreeElement.CASE_ID_EXPR.getReference();
        @Override
        public QuerySetLookup getTransformedLookup(QuerySetLookup incoming,
                                                   TreeReference relativeLookup) {
            if (caseIdRef.equals(relativeLookup)) {
                return incoming;
            } else {
                return null;
            }
        }
    }
}

