package org.javarosa.core.model;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.DataUtil;
import org.javarosa.xpath.XPathTypeMismatchException;

import java.util.List;
import java.util.Vector;

public class FormDefUtils {
    public static QuestionDef findQuestionByRef(TreeReference ref, IFormElement fe) {
        if (fe instanceof FormDef) {
            ref = ref.genericize();
        }

        if (fe instanceof QuestionDef) {
            QuestionDef q = (QuestionDef)fe;
            TreeReference bind = FormInstance.unpackReference(q.getBind());
            return (ref.equals(bind) ? q : null);
        } else {
            for (int i = 0; i < fe.getChildren().size(); i++) {
                QuestionDef ret = findQuestionByRef(ref, fe.getChild(i));
                if (ret != null)
                    return ret;
            }
            return null;
        }
    }

    public static FormIndex buildIndex(Vector<Integer> indexes, Vector<Integer> multiplicities, Vector<IFormElement> elements) {
        FormIndex cur = null;
        Vector<Integer> curMultiplicities = new Vector<>();
        for (int j = 0; j < multiplicities.size(); ++j) {
            curMultiplicities.addElement(multiplicities.elementAt(j));
        }

        Vector<IFormElement> curElements = new Vector<>();
        for (int j = 0; j < elements.size(); ++j) {
            curElements.addElement(elements.elementAt(j));
        }

        for (int i = indexes.size() - 1; i >= 0; i--) {
            int ix = indexes.elementAt(i);
            int mult = multiplicities.elementAt(i);

            if (!(elements.elementAt(i) instanceof GroupDef && ((GroupDef)elements.elementAt(i)).getRepeat())) {
                mult = -1;
            }

            cur = new FormIndex(cur, ix, mult, getChildInstanceRef(curElements, curMultiplicities));
            curMultiplicities.removeElementAt(curMultiplicities.size() - 1);
            curElements.removeElementAt(curElements.size() - 1);
        }
        return cur;
    }

    /**
     * Return a tree reference which follows the path down the concrete elements provided
     * along with the multiplicities provided.
     */
    public static TreeReference getChildInstanceRef(Vector<IFormElement> elements,
                                                    Vector<Integer> multiplicities) {
        if (elements.size() == 0) {
            return null;
        }

        // get reference for target element
        TreeReference ref = FormInstance.unpackReference(elements.lastElement().getBind()).clone();
        for (int i = 0; i < ref.size(); i++) {
            //There has to be a better way to encapsulate this
            if (ref.getMultiplicity(i) != TreeReference.INDEX_ATTRIBUTE) {
                ref.setMultiplicity(i, 0);
            }
        }

        // fill in multiplicities for repeats along the way
        for (int i = 0; i < elements.size(); i++) {
            IFormElement temp = elements.elementAt(i);
            if (temp instanceof GroupDef && ((GroupDef)temp).getRepeat()) {
                TreeReference repRef = FormInstance.unpackReference(temp.getBind());
                if (repRef.isParentOf(ref, false)) {
                    int repMult = multiplicities.elementAt(i);
                    ref.setMultiplicity(repRef.size() - 1, repMult);
                } else {
                    // question/repeat hierarchy is not consistent with
                    // instance instance and bindings
                    return null;
                }
            }
        }

        return ref;
    }

    public static void collapseIndex(FormDef formDef, FormIndex index,
                                     Vector<Integer> indexes,
                                     Vector<Integer> multiplicities,
                                     Vector<IFormElement> elements) {
        if (!index.isInForm()) {
            return;
        }

        IFormElement element = formDef;
        while (index != null) {
            int i = index.getLocalIndex();
            element = element.getChild(i);

            indexes.addElement(DataUtil.integer(i));
            multiplicities.addElement(DataUtil.integer(index.getInstanceIndex() == -1 ? 0 : index.getInstanceIndex()));
            elements.addElement(element);

            index = index.getNextLevel();
        }
    }

    public static TreeReference getChildInstanceRef(FormDef formDef, FormIndex index) {
        Vector<Integer> indexes = new Vector<>();
        Vector<Integer> multiplicities = new Vector<>();
        Vector<IFormElement> elements = new Vector<>();

        collapseIndex(formDef, index, indexes, multiplicities, elements);
        return getChildInstanceRef(elements, multiplicities);
    }

    /**
     * When a repeat is deleted, we need to reduce the multiplicities of its siblings that were higher than it
     * by one.
     * @param parentElement the parent of the deleted element
     * @param deleteElement the deleted element
     */
    static void reduceTreeSiblingMultiplicities(TreeElement parentElement, TreeElement deleteElement){
        int childMult = deleteElement.getMult();
        // update multiplicities of other child nodes
        for (int i = 0; i < parentElement.getNumChildren(); i++) {
            TreeElement child = parentElement.getChildAt(i);
            // We also need to check that this element matches the deleted element (besides multiplicity)
            // in the case where the deleted repeat's parent isn't a subgroup
            if (child.doFieldsMatch(deleteElement) && child.getMult() > childMult) {
                child.setMult(child.getMult() - 1);
            }
        }
    }

    /**
     * Does the repeat group at the given index enable users to add more items,
     * and if so, has the user reached the item limit?
     *
     * @param repeatRef   Reference pointing to a particular repeat item
     * @param repeatIndex Id for looking up the repeat group
     * @return Do the current constraints on the repeat group allow for adding
     * more children?
     */
    public static boolean canCreateRepeat(FormDef formDef, TreeReference repeatRef, FormIndex repeatIndex) {
        GroupDef repeat = (GroupDef)formDef.getChild(repeatIndex);

        //Check to see if this repeat can have children added by the user
        if (repeat.noAddRemove) {
            //Check to see if there's a count to use to determine how many children this repeat
            //should have
            if (repeat.getCountReference() != null) {
                int currentMultiplicity = repeatIndex.getElementMultiplicity();

                TreeReference absPathToCount = repeat.getConextualizedCountReference(repeatRef);
                AbstractTreeElement countNode = formDef.getMainInstance().resolveReference(absPathToCount);
                if (countNode == null) {
                    throw new XPathTypeMismatchException("Could not find the location " +
                            absPathToCount.toString() + " where the repeat at " +
                            repeatRef.toString(false) + " is looking for its count");
                }
                //get the total multiplicity possible
                IAnswerData boxedCount = countNode.getValue();
                int count;
                if (boxedCount == null) {
                    count = 0;
                } else {
                    try {
                        count = ((Integer)new IntegerData().cast(boxedCount.uncast()).getValue());
                    } catch (IllegalArgumentException iae) {
                        throw new XPathTypeMismatchException("The repeat count value \"" +
                                boxedCount.uncast().getString() +
                                "\" at " + absPathToCount.toString() +
                                " must be a number!");
                    }
                }

                if (count <= currentMultiplicity) {
                    return false;
                }
            } else {
                //Otherwise the user can never add repeat instances
                return false;
            }
        }

        //TODO: If we think the node is still relevant, we also need to figure out a way to test that assumption against
        //the repeat's constraints.


        return true;
    }

    /**
     * Gathers generic children and attribute references for the provided
     * element into the genericRefs list.
     */
    static void addChildrenOfElement(AbstractTreeElement treeElem,
                                     List<TreeReference> genericRefs) {
        // recursively add children of element
        for (int i = 0; i < treeElem.getNumChildren(); ++i) {
            AbstractTreeElement child = treeElem.getChildAt(i);
            TreeReference genericChild = child.getRef().genericize();
            if (!genericRefs.contains(genericChild)) {
                genericRefs.add(genericChild);
            }
            addChildrenOfElement(child, genericRefs);
        }

        // add all the attributes of this element
        for (int i = 0; i < treeElem.getAttributeCount(); ++i) {
            AbstractTreeElement child =
                    treeElem.getAttribute(treeElem.getAttributeNamespace(i),
                            treeElem.getAttributeName(i));
            TreeReference genericChild = child.getRef().genericize();
            if (!genericRefs.contains(genericChild)) {
                genericRefs.add(genericChild);
            }
        }
    }
}
