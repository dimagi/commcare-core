package org.commcare.cases.query.queryset;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathEqExpr;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import sun.reflect.generics.tree.Tree;

/**
 * Created by ctsims on 2/6/2017.
 */

public class CaseModelQuerySetMatcher implements ModelQuerySetMatcher {
    private final Collection<XPathExpression> membershipIndexes;

    private TreeReference caseDbRoot;
    private Map<Integer, Integer> multiplicityMap;

    private Vector<QuerySetTransform> querySetTransforms = new Vector<>();

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
        querySetTransforms.add(new CaseIdentityQuerySetTransform());
    }

    public void addQuerySetTransform(QuerySetTransform transform) {
        this.querySetTransforms.add(transform);
    }

    @Override
    public QuerySetLookup getQueryLookupFromPredicate(XPathExpression expr) {
        if(expr instanceof XPathEqExpr && ((XPathEqExpr)expr).op == XPathEqExpr.EQ) {
            XPathEqExpr eq = ((XPathEqExpr)expr);
            if(membershipIndexes.contains(eq.a)) {
                if(eq.b instanceof XPathPathExpr) {
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

        if(caseDbRoot.isParentOf(ref, false)) {
            if(!ref.hasPredicates()) {
                return null;
            }

            List<XPathExpression> predicates = ref.getPredicate(caseDbRoot.size() - 1);
            if(predicates == null || predicates.size() > 1) {
                return null;
            }

            lookup = getQueryLookupFromPredicate(predicates.get(0));
            if(lookup == null) {
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

        for(QuerySetTransform transform : querySetTransforms) {
            QuerySetLookup retVal = transform.getTransformedLookup(lookup, remainder);
            if(retVal != null) {
                return retVal;
            }
        }
        return null;
    }

    private boolean isCurrentRef(TreeReference ref) {
        return ref.getContext() == TreeReference.CONTEXT_ORIGINAL;
    }

    private static class CaseIdentityQuerySetTransform implements QuerySetTransform {
        static TreeReference caseIdRef = CaseInstanceTreeElement.CASE_ID_EXPR.getReference();
        @Override
        public QuerySetLookup getTransformedLookup(QuerySetLookup incoming,
                                                   TreeReference relativeLookup) {
            if(caseIdRef.equals(relativeLookup)) {
                return incoming;
            } else {
                return null;
            }
        }
    }
}

