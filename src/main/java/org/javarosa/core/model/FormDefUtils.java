package org.javarosa.core.model;

import org.javarosa.core.log.WrappedException;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.condition.Triggerable;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.util.DataUtil;
import org.javarosa.xpath.XPathTypeMismatchException;

import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

public class FormDefUtils {
    private static final int TEMPLATING_RECURSION_LIMIT = 10;

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

    public static int getNumRepetitions(FormDef formDef, FormIndex index) {
        Vector<Integer> indexes = new Vector<>();
        Vector<Integer> multiplicities = new Vector<>();
        Vector<IFormElement> elements = new Vector<>();

        if (!index.isInForm()) {
            throw new RuntimeException("not an in-form index");
        }

        collapseIndex(formDef, index, indexes, multiplicities, elements);

        if (!(elements.lastElement() instanceof GroupDef) || !((GroupDef)elements.lastElement()).getRepeat()) {
            throw new RuntimeException("current element not a repeat");
        }

        //so painful
        TreeElement templNode = formDef.getMainInstance().getTemplate(index.getReference());
        TreeReference parentPath = templNode.getParent().getRef().genericize();
        TreeElement parentNode = formDef.getMainInstance().resolveReference(parentPath.contextualize(index.getReference()));
        return parentNode.getChildMultiplicity(templNode.getName());
    }

    //repIndex == -1 => next repetition about to be created
    public static FormIndex descendIntoRepeat(FormDef formDef, FormIndex index, int repIndex) {
        int numRepetitions = getNumRepetitions(formDef, index);

        Vector<Integer> indexes = new Vector<>();
        Vector<Integer> multiplicities = new Vector<>();
        Vector<IFormElement> elements = new Vector<>();
        collapseIndex(formDef, index, indexes, multiplicities, elements);

        if (repIndex == -1) {
            repIndex = numRepetitions;
        } else {
            if (repIndex < 0 || repIndex >= numRepetitions) {
                throw new RuntimeException("selection exceeds current number of repetitions");
            }
        }

        multiplicities.setElementAt(DataUtil.integer(repIndex), multiplicities.size() - 1);

        return buildIndex(indexes, multiplicities, elements);
    }

    static void initEvalContext(final FormDef f, EvaluationContext ec) {
        if (!ec.getFunctionHandlers().containsKey("jr:itext")) {
            ec.addFunctionHandler(new IFunctionHandler() {
                @Override
                public String getName() {
                    return "jr:itext";
                }

                @Override
                public Object eval(Object[] args, EvaluationContext ec) {
                    String textID = (String)args[0];
                    try {
                        //SUUUUPER HACKY
                        String form = ec.getOutputTextForm();
                        if (form != null) {
                            textID = textID + ";" + form;
                            String result = f.getLocalizer().getRawText(f.getLocalizer().getLocale(), textID);
                            return result == null ? "" : result;
                        } else {
                            String text = f.getLocalizer().getText(textID);
                            return text == null ? "[itext:" + textID + "]" : text;
                        }
                    } catch (NoSuchElementException nsee) {
                        return "[nolocale]";
                    }
                }

                @Override
                public Vector getPrototypes() {
                    Class[] proto = {String.class};
                    Vector<Class[]> v = new Vector<>();
                    v.addElement(proto);
                    return v;
                }

                @Override
                public boolean rawArgs() {
                    return false;
                }
            });
        }

        /* function to reverse a select value into the display label for that choice in the question it came from
         *
         * arg 1: select value
         * arg 2: string xpath referring to origin question; must be absolute path
         *
         * this won't work at all if the original label needed to be processed/calculated in some way (<output>s, etc.) (is this even allowed?)
         * likely won't work with multi-media labels
         * _might_ work for itemsets, but probably not very well or at all; could potentially work better if we had some context info
         * DOES work with localization
         *
         * it's mainly intended for the simple case of reversing a question with compile-time-static fields, for use inside an <output>
         */
        if (!ec.getFunctionHandlers().containsKey("jr:choice-name")) {
            ec.addFunctionHandler(new IFunctionHandler() {
                @Override
                public String getName() {
                    return "jr:choice-name";
                }

                @Override
                public Object eval(Object[] args, EvaluationContext ec) {
                    try {
                        String value = (String)args[0];
                        String questionXpath = (String)args[1];
                        TreeReference ref = RestoreUtils.ref(questionXpath);

                        QuestionDef q = findQuestionByRef(ref, f);
                        if (q == null || (q.getControlType() != Constants.CONTROL_SELECT_ONE &&
                                q.getControlType() != Constants.CONTROL_SELECT_MULTI)) {
                            return "";
                        }

                        System.out.println("here!!");

                        Vector<SelectChoice> choices = q.getChoices();
                        for (SelectChoice ch : choices) {
                            if (ch.getValue().equals(value)) {
                                //this is really not ideal. we should hook into the existing code (FormEntryPrompt) for pulling
                                //display text for select choices. however, it's hard, because we don't really have
                                //any context to work with, and all the situations where that context would be used
                                //don't make sense for trying to reverse a select value back to a label in an unrelated
                                //expression

                                String textID = ch.getTextID();
                                if (textID != null) {
                                    return f.getLocalizer().getText(textID);
                                } else {
                                    return ch.getLabelInnerText();
                                }
                            }
                        }
                        return "";
                    } catch (Exception e) {
                        throw new WrappedException("error in evaluation of xpath function [choice-name]", e);
                    }
                }

                @Override
                public Vector getPrototypes() {
                    Class[] proto = {String.class, String.class};
                    Vector<Class[]> v = new Vector<>();
                    v.addElement(proto);
                    return v;
                }

                @Override
                public boolean rawArgs() {
                    return false;
                }
            });
        }
    }

    /**
     * Performs substitutions on place-holder template from form text by
     * evaluating args in template using the current context.
     *
     * @param template   String
     * @param contextRef TreeReference
     * @param variables  Hashtable<String, ?>
     * @return String with the all args in the template filled with appropriate
     * context values.
     */
    public static String fillTemplateString(FormDef formDef, String template,
                                            TreeReference contextRef,
                                            Hashtable<String, ?> variables) {
        // argument to value mapping
        Hashtable<String, String> args = new Hashtable<>();

        int depth = 0;
        // grab all template arguments that need to have substitutions performed
        Vector outstandingArgs = Localizer.getArgs(template);

        String templateAfterSubstitution;

        // Step through outstandingArgs from the template, looking up the value
        // they map to, evaluating that under the evaluation context and
        // storing in the local args mapping.
        // Then perform substitutions over the template until a fixpoint is found
        while (outstandingArgs.size() > 0) {
            for (int i = 0; i < outstandingArgs.size(); i++) {
                String argName = (String)outstandingArgs.elementAt(i);
                // lookup value an arg points to if it isn't in our local mapping
                if (!args.containsKey(argName)) {
                    int ix = -1;
                    try {
                        ix = Integer.parseInt(argName);
                    } catch (NumberFormatException nfe) {
                        System.err.println("Warning: expect arguments to be numeric [" + argName + "]");
                    }

                    if (ix < 0 || ix >= formDef.getOutputFragments().size()) {
                        continue;
                    }

                    IConditionExpr expr = formDef.getOutputFragments().elementAt(ix);
                    EvaluationContext ec = new EvaluationContext(formDef.getEvaluationContext(), contextRef);
                    ec.setOriginalContext(contextRef);
                    ec.setVariables(variables);
                    String value = expr.evalReadable(formDef.getMainInstance(), ec);
                    args.put(argName, value);
                }
            }

            templateAfterSubstitution = Localizer.processArguments(template, args);

            // The last substitution made no progress, probably because the
            // argument isn't in outputFragments, so stop looping and
            // attempting more subs!
            if (template.equals(templateAfterSubstitution)) {
                return template;
            }

            template = templateAfterSubstitution;

            // Since strings being substituted might themselves have arguments that
            // need to be further substituted, we must recompute the unperformed
            // substitutions and continue to loop.
            outstandingArgs = Localizer.getArgs(template);

            if (depth++ >= TEMPLATING_RECURSION_LIMIT) {
                throw new RuntimeException("Dependency cycle in <output>s; recursion limit exceeded!!");
            }
        }

        return template;
    }

    static void throwGraphCyclesException(List<Triggerable> vertices) {
        String hints = "";
        for (Triggerable t : vertices) {
            for (TreeReference r : t.getTargets()) {
                hints += "\n" + r.toString(true);
            }
        }
        String message = "Cycle detected in form's relevant and calculation logic!";
        if (!hints.equals("")) {
            message += "\nThe following nodes are likely involved in the loop:" + hints;
        }
        throw new IllegalStateException(message);
    }
}
