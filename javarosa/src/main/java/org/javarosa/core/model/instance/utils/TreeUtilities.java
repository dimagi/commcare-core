package org.javarosa.core.model.instance.utils;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.CacheTable;
import org.javarosa.core.util.DataUtil;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.expr.XPathEqExpr;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.expr.XPathStep;
import org.javarosa.xpath.expr.XPathStringLiteral;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Helper methods for procedures which are common to different Tree model
 * implementations and don't fit well into the inheritance model
 *
 * @author ctsims
 */
public class TreeUtilities {

    /**
     * A general purpose method for taking an abstract tree element and
     * attempting to batch fetch its children's predicates through static
     * evaluation.
     *
     * @param parent                The element whose children are being requested
     * @param childAttributeHintMap A mapping of paths which can be evaluated in memory.
     * @param name                  The name of the children being queried
     * @param mult                  The multiplicity being queried for (could be undefined)
     * @param predicates            The evaluation step predicates which are
     *                              being processed. NOTE: This vector will be modified by this method as a
     *                              side effect if a predicate was succesfully statically evaluated
     * @param evalContext           The current eval context.
     * @return A vector of TreeReferences which contains the nodes matched by predicate expressions.
     * Expressions which result in returned matches will be removed from the predicate collection which
     * is provided
     */
    public static Vector<TreeReference> tryBatchChildFetch(AbstractTreeElement parent,
                                                           Hashtable<XPathPathExpr,
                                                                   Hashtable<String, TreeElement[]>> childAttributeHintMap,
                                                           String name,
                                                           int mult,
                                                           Vector<XPathExpression> predicates,
                                                           EvaluationContext evalContext) {
        // This method builds a predictive model for quick queries that
        // prevents the need to fully flesh out full walks of the tree.

        // TODO: We build a bunch of models here, it's not clear whether we
        // should be retaining them for multiple queries in the future rather
        // than letting it rebuild the same caches a couple of times

        // We also need to figure out exactly how to determine whether this
        // "worked" more or less and potentially preventing this attempt from
        // proceeding in the future, since it's not exactly free...

        // Only do for predicates
        if (mult != TreeReference.INDEX_UNBOUND || predicates == null) {
            return null;
        }

        Vector<Integer> toRemove = new Vector<>();
        Vector<TreeReference> allSelectedChildren = null;

        //Lazy init these until we've determined that our predicate is hintable

        //These two are basically a map, but we dont' have a great datatype for this
        Vector<String> attributes = null;
        Vector<XPathPathExpr> indices = null;

        Vector<TreeElement> kids = null;

        predicate:
        for (int i = 0; i < predicates.size(); ++i) {
            Vector<TreeReference> predicateMatches = new Vector<>();
            XPathExpression xpe = predicates.elementAt(i);
            //what we want here is a static evaluation of the expression to see if it consists of evaluating
            //something we index with something static.
            if (xpe instanceof XPathEqExpr) {
                XPathExpression left = ((XPathEqExpr)xpe).a;
                XPathExpression right = ((XPathEqExpr)xpe).b;

                //For now, only cheat when this is a string literal (this basically just means that we're
                //handling attribute based referencing with very reasonable timing, but it's complex otherwise)
                if (left instanceof XPathPathExpr && (right instanceof XPathStringLiteral || right instanceof XPathPathExpr)) {
                    String literalMatch = null;
                    if (right instanceof XPathStringLiteral) {
                        literalMatch = ((XPathStringLiteral)right).s;
                    } else if (right instanceof XPathPathExpr) {
                        //We'll also try to match direct path queries as long as they are not
                        //complex.

                        //First: Evaluate whether there are predicates (which may have nesting that ruins our ability to do this)
                        for (XPathStep step : ((XPathPathExpr)right).steps) {
                            if (step.predicates.length > 0) {
                                //We can't evaluate this, just bail
                                break;
                            }
                        }

                        try {
                            //Otherwise, go pull out the right hand value
                            Object o = XPathFuncExpr.unpack(right.eval(evalContext));
                            literalMatch = XPathFuncExpr.toString(o);
                        } catch (XPathException e) {
                            //We may have some weird lack of context that makes this not work, so don't choke on the bonus evaluation
                            //and just evaluate that traditional way
                            e.printStackTrace();
                            break;
                        }
                    }

                    //First, see if we can run this query with a hint map, rather than jumping out to storage
                    //since that may involve iterative I/O queries.
                    if (childAttributeHintMap != null) {
                        if (childAttributeHintMap.containsKey(left)) {

                            //Retrieve the list of children which match our literal
                            TreeElement[] children = childAttributeHintMap.get(left).get(literalMatch);
                            if (children != null) {
                                for (TreeElement element : children) {
                                    predicateMatches.addElement(element.getRef());
                                }
                            }
                            //Merge and note that this predicate is evaluated and doesn't need to be evaluated in the future.
                            allSelectedChildren = merge(allSelectedChildren, predicateMatches, i, toRemove);
                            continue predicate;
                        }
                    }


                    //We're lazily initializing this, since it might actually take a while, and we
                    //don't want the overhead if our predicate is too complex anyway

                    //TODO: Probably makes sense to actually just build the hint mapping here,
                    //but we currently don't robustly track changes to the models, so would
                    //be too dangerous at the moment
                    if (attributes == null) {
                        attributes = new Vector<>();
                        indices = new Vector<>();
                        kids = parent.getChildrenWithName(name);

                        if (kids.size() == 0) {
                            return null;
                        }

                        //Anything that we're going to use across elements should be on all of them
                        AbstractTreeElement kid = kids.elementAt(0);
                        for (int j = 0; j < kid.getAttributeCount(); ++j) {
                            String attribute = kid.getAttributeName(j);
                            XPathPathExpr path = TreeUtilities.getXPathAttrExpression(attribute);
                            attributes.addElement(attribute);
                            indices.addElement(path);
                        }
                    }

                    for (int j = 0; j < indices.size(); ++j) {
                        XPathPathExpr expr = indices.elementAt(j);
                        if (expr.equals(left)) {
                            String attributeName = attributes.elementAt(j);

                            for (int kidI = 0; kidI < kids.size(); ++kidI) {
                                String attrValue = kids.elementAt(kidI).getAttributeValue(null, attributeName);

                                // We don't necessarily having typing
                                // information for these attributes (and if we
                                // did it's not available here) so we will try
                                // to do some _very basic_ type inference on
                                // this value before performing the match
                                Object value = XPathFuncExpr.InferType(attrValue);

                                if (XPathEqExpr.testEquality(value, literalMatch)) {
                                    predicateMatches.addElement(kids.elementAt(kidI).getRef());
                                }
                            }

                            // Merge and note that this predicate is evaluated
                            // and doesn't need to be evaluated in the future.
                            allSelectedChildren = merge(allSelectedChildren, predicateMatches, i, toRemove);
                            continue predicate;
                        }
                    }
                }
            }
            // There's only one case where we want to keep moving along, and we
            // would have triggered it if it were going to happen, so
            // otherwise, just get outta here.
            break;
        }

        // if we weren't able to evaluate any predicates, signal that.
        if (allSelectedChildren == null) {
            return null;
        }

        // otherwise, remove all of the predicates we've already evaluated
        for (int i = toRemove.size() - 1; i >= 0; i--) {
            predicates.removeElementAt(toRemove.elementAt(i));
        }

        return allSelectedChildren;
    }


    private static Vector<TreeReference> merge(Vector<TreeReference> allSelectedChildren,
                                               Vector<TreeReference> predicateMatches,
                                               int i, Vector<Integer> toRemove) {
        toRemove.addElement(DataUtil.integer(i));
        if (allSelectedChildren == null) {
            return predicateMatches;
        }
        return DataUtil.intersection(allSelectedChildren, predicateMatches);
    }


    //Static XPathPathExpr cache. Not 100% clear whether this is the best caching strategy, but it's the easiest.
    static final CacheTable<String, XPathPathExpr> table = new CacheTable<>();

    public static XPathPathExpr getXPathAttrExpression(String attribute) {
        //Cache tables can only take in integers due to some terrible 1.3 design issues
        //so we have to manually cache our attribute string's hash and follow from there.
        XPathPathExpr cached = table.retrieve(attribute);

        if (cached == null) {
            cached = XPathReference.getPathExpr("@" + attribute);
            table.register(attribute, cached);
        }
        return cached;
    }
}
