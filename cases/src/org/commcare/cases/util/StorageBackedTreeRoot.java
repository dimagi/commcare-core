/**
 * 
 */
package org.commcare.cases.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.DataUtil;
import org.javarosa.xpath.expr.XPathEqExpr;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathPathExpr;


/**
 * @author ctsims
 *
 */
public abstract class StorageBackedTreeRoot<T extends AbstractTreeElement> implements AbstractTreeElement<T> {
    
    protected Hashtable<Integer, Integer> objectIdMapping;
    
    protected abstract String getChildHintName();
    
    protected abstract Hashtable<XPathPathExpr, String> getStorageIndexMap();
    
    protected abstract IStorageUtilityIndexed<?> getStorage();
    
    protected Vector<Integer> union(Vector<Integer> selectedCases, Vector<Integer> cases) {
        return DataUtil.union(selectedCases, cases);
    }
    
    protected abstract void initStorageCache();
    
    protected String translateFilterExpr(XPathPathExpr expressionTemplate, XPathPathExpr matchingExpr, Hashtable<XPathPathExpr, String> indices) {
        return indices.get(expressionTemplate);
    }
    
    /**
     * Gets a potential cached mapping from a storage key that could be queried
     * on this tree to the storage ID of that element, rather than querying for
     * that through I/O.
     * 
     * @param keyId The ID of a storage metadata key.
     * @return A table mapping the metadata key (must be unique) to the id of a 
     * record in the storage backing this tree root.
     */
    protected Hashtable<String, Integer> getKeyMapping(String keyId) {
        return null;
    }
    
    public Vector<TreeReference> tryBatchChildFetch(String name, int mult, Vector<XPathExpression> predicates, EvaluationContext evalContext) {
        //Restrict what we'll handle for now. All we want to deal with is predicate expressions on case blocks
        if(!name.equals(getChildHintName()) || mult != TreeReference.INDEX_UNBOUND || predicates == null) { return null; }
        
        Vector<Integer> selectedElements = null;
        Vector<Integer> toRemove = new Vector<Integer>();
        
        IStorageUtilityIndexed<?> storage= getStorage();
        Hashtable<XPathPathExpr, String> indices = getStorageIndexMap();
        
        predicate:
        for(int i = 0 ; i < predicates.size() ; ++i) {
            XPathExpression xpe = predicates.elementAt(i);
            //what we want here is a static evaluation of the expression to see if it consists of evaluating 
            //something we index with something static.
            if(xpe instanceof XPathEqExpr) {
                XPathExpression left = ((XPathEqExpr)xpe).a;
                if(left instanceof XPathPathExpr) {
                    for(Enumeration en = indices.keys(); en.hasMoreElements() ;) {
                        XPathPathExpr expr = (XPathPathExpr)en.nextElement();
                        if(expr.matches(left)) {                            
                            String filterIndex = translateFilterExpr(expr, (XPathPathExpr)left, indices);

                            //TODO: We need a way to determine that this value does not also depend on anything in the current context, not 
                            //sure the best way to do that....? Maybe tell the evaluation context to skip out here if it detects a request
                            //to resolve in a certain area?
                            Object o = XPathFuncExpr.unpack(((XPathEqExpr)xpe).b.eval(evalContext));
                            
                            //Some storage roots will collect common iterative mappings ahead of time,
                            //go check whether this key is loaded into cached memory.
                            Hashtable<String, Integer> keyMapping = getKeyMapping(filterIndex);
                            if(keyMapping != null) {
                                //If so, go fetch that element's record id and skip the storage
                                //lookup
                                Integer uniqueValue = keyMapping.get(XPathFuncExpr.toString(o));
                                
                                if(uniqueValue != null) {
                                    if(selectedElements == null) {
                                        selectedElements = new Vector<Integer>();
                                        selectedElements.addElement(uniqueValue);
                                    } else {
                                        if(!selectedElements.contains(uniqueValue)) {
                                            selectedElements.addElement(uniqueValue);
                                        }
                                    }
                                }
                            } else {
                                Vector<Integer> cases = null;

                                try{
                                    //Get all of the cases that meet this criteria
                                    cases = storage.getIDsForValue(filterIndex, o);
                                } catch(IllegalArgumentException IAE) {
                                    //We can only get this if we have a new index type
                                    storage.registerIndex(filterIndex);
                                    try{
                                        cases = storage.getIDsForValue(filterIndex, o);
                                    } catch(IllegalArgumentException iaeagain) {
                                        //Still didn't work, platform can't expand indices
                                        break predicate;
                                    }
                                }
                                // merge with any other sets of cases
                                if(selectedElements == null) {
                                    selectedElements = cases;
                                } else {
                                    selectedElements = union(selectedElements, cases);
                                }
                            }
                            
                            //Note that this predicate is evaluated and doesn't need to be evaluated in the future.
                            toRemove.addElement(DataUtil.integer(i));
                            continue predicate;
                        }
                    }
                }
            }
            //There's only one case where we want to keep moving along, and we would have triggered it if it were going to happen,
            //so otherwise, just get outta here.
            break;
        }
        
        //if we weren't able to evaluate any predicates, signal that.
        if(selectedElements == null) { return null; }
        
        //otherwise, remove all of the predicates we've already evaluated
        for(int i = toRemove.size() - 1; i >= 0 ; i--)  {
            predicates.removeElementAt(toRemove.elementAt(i).intValue());
        }
        
        TreeReference base = this.getRef();
        
        initStorageCache();

        Vector<TreeReference> filtered = new Vector<TreeReference>();
        for(Integer i : selectedElements) {
            //this takes _waaaaay_ too long, we need to refactor this
            TreeReference ref = base.clone();
            Integer realIndexInt = objectIdMapping.get(i);
            int realIndex =realIndexInt.intValue();
            ref.add(this.getChildHintName(), realIndex);
            filtered.addElement(ref);
        }
        return filtered;
    }
    

}
