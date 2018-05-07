package org.javarosa.form.api;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.instance.InvalidReferenceException;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.core.model.trace.EvaluationTraceSerializer;
import org.javarosa.xpath.XPathTypeMismatchException;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The data model used during form entry. Represents the current state of the
 * form and provides access to the objects required by the view and the
 * controller.
 */
public class FormEntryModel {
    private final FormDef form;
    private FormIndex currentFormIndex;

    /**
     * One of "REPEAT_STRUCUTRE_" in this class's static types,
     * represents what abstract structure repeat events should
     * be broadacast as.
     */
    private int repeatStructure = -1;

    /**
     * Repeats should be a prompted linear set of questions, either
     * with a fixed set of repetitions, or a prompt for creating a
     * new one.
     */
    public static final int REPEAT_STRUCTURE_LINEAR = 1;

    /**
     * Repeats should be a custom juncture point with centralized
     * "Create/Remove/Interact" hub.
     */
    public static final int REPEAT_STRUCTURE_NON_LINEAR = 2;


    public FormEntryModel(FormDef form) {
        this(form, REPEAT_STRUCTURE_LINEAR);
    }

    /**
     * Creates a new entry model for the form with the appropriate
     * repeat structure
     *
     * @param repeatStructure The structure of repeats (the repeat signals which should
     *                        be sent during form entry)
     * @throws IllegalArgumentException If repeatStructure is not valid
     */
    public FormEntryModel(FormDef form, int repeatStructure) {
        this.form = form;
        if (repeatStructure != REPEAT_STRUCTURE_LINEAR && repeatStructure != REPEAT_STRUCTURE_NON_LINEAR) {
            throw new IllegalArgumentException(repeatStructure + ": does not correspond to a valid repeat structure");
        }
        //We need to see if there are any guessed repeat counts in the form, which prevents
        //us from being able to use the new repeat style
        //Unfortunately this is probably (A) slow and (B) might overflow the stack. It's not the only
        //recursive walk of the form, though, so (B) isn't really relevant
        if (repeatStructure == REPEAT_STRUCTURE_NON_LINEAR && containsRepeatGuesses(form)) {
            repeatStructure = REPEAT_STRUCTURE_LINEAR;
        }
        this.repeatStructure = repeatStructure;
        this.currentFormIndex = FormIndex.createBeginningOfFormIndex();
    }

    /**
     * Given a FormIndex, returns the event this FormIndex represents.
     *
     * @see FormEntryController
     */
    public int getEvent(FormIndex index) {
        if (index.isBeginningOfFormIndex()) {
            return FormEntryController.EVENT_BEGINNING_OF_FORM;
        } else if (index.isEndOfFormIndex()) {
            return FormEntryController.EVENT_END_OF_FORM;
        }

        IFormElement element = form.getChild(index);
        if (element instanceof GroupDef) {
            if (((GroupDef)element).isRepeat()) {
                if (repeatStructure != REPEAT_STRUCTURE_NON_LINEAR && form.getMainInstance().resolveReference(form.getChildInstanceRef(index)) == null) {
                    return FormEntryController.EVENT_PROMPT_NEW_REPEAT;
                } else if (repeatStructure == REPEAT_STRUCTURE_NON_LINEAR && index.getElementMultiplicity() == TreeReference.INDEX_REPEAT_JUNCTURE) {
                    return FormEntryController.EVENT_REPEAT_JUNCTURE;
                } else {
                    return FormEntryController.EVENT_REPEAT;
                }
            } else {
                return FormEntryController.EVENT_GROUP;
            }
        } else {
            return FormEntryController.EVENT_QUESTION;
        }
    }

    protected TreeElement getTreeElement(FormIndex index) {
        return form.getMainInstance().resolveReference(index.getReference());
    }

    /**
     * @return the event for the current FormIndex
     * @see FormEntryController
     */
    public int getEvent() {
        return getEvent(currentFormIndex);
    }

    public String getFormTitle() {
        return form.getTitle();
    }

    /**
     * @return Returns the FormEntryPrompt for the specified FormIndex if the
     * index represents a question.
     */
    public FormEntryPrompt getQuestionPrompt(FormIndex index) {
        if (form.getChild(index) instanceof QuestionDef) {
            return new FormEntryPrompt(form, index);
        } else {
            throw new RuntimeException(
                    "Invalid query for Question prompt. Non-Question object at the form index " + index + " in Form " + form.getName());
        }
    }

    /**
     * @return Returns the FormEntryPrompt for the current FormIndex if the
     * index represents a question.
     */
    public FormEntryPrompt getQuestionPrompt() {
        return getQuestionPrompt(currentFormIndex);
    }

    /**
     * When you have a non-question event, a CaptionPrompt will have all the
     * information needed to display to the user.
     *
     * @return Returns the FormEntryCaption for the given FormIndex if is not a
     * question.
     */
    public FormEntryCaption getCaptionPrompt(FormIndex index) {
        return new FormEntryCaption(form, index);
    }

    /**
     * When you have a non-question event, a CaptionPrompt will have all the
     * information needed to display to the user.
     *
     * @return Returns the FormEntryCaption for the current FormIndex if is not
     * a question.
     */
    public FormEntryCaption getCaptionPrompt() {
        return getCaptionPrompt(currentFormIndex);
    }

    /**
     * @return an array of Strings of the current langauges. Null if there are
     * none.
     */
    public String[] getLanguages() {
        if (form.getLocalizer() != null) {
            return form.getLocalizer().getAvailableLocales();
        }
        return null;
    }

    /**
     * @return Returns the current FormIndex referenced by the FormEntryModel.
     */
    public FormIndex getFormIndex() {
        return currentFormIndex;
    }


    protected void setLanguage(String language) {
        if (form.getLocalizer() != null) {
            form.getLocalizer().setLocale(language);
        }
    }

    /**
     * @return Returns the currently selected language.
     */
    public String getLanguage() {
        return form.getLocalizer().getLocale();
    }


    public void setQuestionIndex(FormIndex index) {
        this.setQuestionIndex(index, true);
    }

    /**
     * Set the FormIndex for the current question.
     *
     * @param expandRepeats Expand any unexpanded repeat groups
     */
    public void setQuestionIndex(FormIndex index, boolean expandRepeats) {
        if (!currentFormIndex.equals(index)) {
            // See if a hint exists that says we should have a model for this
            // already
            if (expandRepeats) {
                createModelIfNecessary(index);
            }
            currentFormIndex = index;
        }
    }

    public FormDef getForm() {
        return form;
    }

    /**
     * Returns a hierarchical list of FormEntryCaption objects for the given
     * FormIndex
     *
     * @return list of FormEntryCaptions in hierarchical order
     */
    public FormEntryCaption[] getCaptionHierarchy(FormIndex index) {
        Vector<FormEntryCaption> captions = new Vector<>();
        FormIndex remaining = index;
        while (remaining != null) {
            remaining = remaining.getNextLevel();
            FormIndex localIndex = index.diff(remaining);
            IFormElement element = form.getChild(localIndex);
            if (element != null) {
                FormEntryCaption caption = null;
                if (element instanceof GroupDef)
                    caption = new FormEntryCaption(getForm(), localIndex);
                else if (element instanceof QuestionDef)
                    caption = new FormEntryPrompt(getForm(), localIndex);

                if (caption != null) {
                    captions.addElement(caption);
                }
            }
        }
        FormEntryCaption[] captionArray = new FormEntryCaption[captions.size()];
        captions.copyInto(captionArray);
        return captionArray;
    }

    /**
     * Returns a hierarchical list of FormEntryCaption objects for the current
     * FormIndex
     *
     * @return list of FormEntryCaptions in hierarchical order
     */
    public FormEntryCaption[] getCaptionHierarchy() {
        return getCaptionHierarchy(currentFormIndex);
    }

    /**
     * Determine if the current FormIndex is relevant. Only relevant indexes
     * should be returned when filling out a form.
     *
     * @return true if current element at FormIndex is relevant
     */
    public boolean isIndexRelevant(FormIndex index) {
        TreeReference ref = form.getChildInstanceRef(index);
        boolean isAskNewRepeat = (getEvent(index) == FormEntryController.EVENT_PROMPT_NEW_REPEAT);
        boolean isRepeatJuncture = (getEvent(index) == FormEntryController.EVENT_REPEAT_JUNCTURE);

        boolean relevant;
        if (isAskNewRepeat) {
            relevant = form.isRepeatRelevant(ref) && form.canCreateRepeat(ref, index);
            //repeat junctures are still relevant if no new repeat can be created; that option
            //is simply missing from the menu
        } else if (isRepeatJuncture) {
            relevant = form.isRepeatRelevant(ref);
        } else {
            TreeElement node = form.getMainInstance().resolveReference(ref);
            relevant = (node != null) && node.isRelevant();
        }

        if (relevant) { // if instance flag/condition says relevant, we still
            // have to check the <group>/<repeat> hierarchy

            FormIndex ancestorIndex = index;
            while (!ancestorIndex.isTerminal()) {
                // This should be safe now that the TreeReference is contained
                // in the ancestor index itself
                TreeElement ancestorNode =
                        form.getMainInstance().resolveReference(ancestorIndex.getLocalReference());

                if (!ancestorNode.isRelevant()) {
                    relevant = false;
                    break;
                }
                ancestorIndex = ancestorIndex.getNextLevel();
            }
        }

        return relevant;
    }

    /**
     * For the current index: Checks whether the index represents a node which
     * should exist given a non-interactive repeat, along with a count for that
     * repeat which is beneath the dynamic level specified.
     *
     * If this index does represent such a node, the new model for the repeat is
     * created behind the scenes and the index for the initial question is
     * returned.
     *
     * Note: This method will not prevent the addition of new repeat elements in
     * the interface, it will merely use the xforms repeat hint to create new
     * nodes that are assumed to exist
     *
     * @param index To be evaluated as to whether the underlying model is
     *              hinted to exist
     */
    public void createModelIfNecessary(FormIndex index) {
        if (index.isInForm()) {
            IFormElement e = getForm().getChild(index);
            if (e instanceof GroupDef) {
                GroupDef g = (GroupDef)e;
                createModelForGroup(g, index, getForm());
            }
        }
    }

    private static void createModelForGroup(GroupDef g, FormIndex index, FormDef form) {
        if (g.isRepeat() && g.getCountReference() != null) {
            TreeReference countRef = g.getConextualizedCountReference(index.getReference());
            IAnswerData count = form.getMainInstance().resolveReference(countRef).getValue();
            if (count != null) {
                int fullcount;
                try {
                    fullcount = ((Integer)new IntegerData().cast(count.uncast()).getValue());
                } catch (IllegalArgumentException iae) {
                    throw new XPathTypeMismatchException("The repeat count value \""
                            + count.uncast().getString() + "\" at "
                            + g.getConextualizedCountReference(index.getReference()).toString()
                            + " must be a number!");
                }

                createModelIfBelowMaxCount(index, form, fullcount);
            }
        }
    }

    private static void createModelIfBelowMaxCount(FormIndex index, FormDef form, int fullcount) {
        TreeReference ref = form.getChildInstanceRef(index);
        TreeElement element = form.getMainInstance().resolveReference(ref);
        if (element == null) {
            int instanceIndexOfDeepestRepeat = index.getLastRepeatInstanceIndex();
            if (instanceIndexOfDeepestRepeat == -1) {
                throw new RuntimeException("Attempting to expand a repeat for a form index where no repeats were present: " + index);
            }
            if (instanceIndexOfDeepestRepeat < fullcount) {
                try {
                    form.createNewRepeat(index);
                } catch (InvalidReferenceException ire) {
                    ire.printStackTrace();
                    throw new RuntimeException("Invalid Reference while creating new repeat!" + ire.getMessage());
                }
            }
        }
    }

    public FormIndex incrementIndex(FormIndex index, boolean descend) {
        Vector<Integer> indexes = new Vector<>();
        Vector<Integer> multiplicities = new Vector<>();
        Vector<IFormElement> elements = new Vector<>();

        if (index.isEndOfFormIndex()) {
            return index;
        } else if (index.isBeginningOfFormIndex()) {
            if (form.getChildren() == null || form.getChildren().size() == 0) {
                return FormIndex.createEndOfFormIndex();
            }
        } else {
            form.collapseIndex(index, indexes, multiplicities, elements);
        }

        incrementHelper(indexes, multiplicities, elements, descend);

        if (indexes.size() == 0) {
            return FormIndex.createEndOfFormIndex();
        } else {
            return form.buildIndex(indexes, multiplicities, elements);
        }
    }

    private void incrementHelper(Vector<Integer> indexes, Vector<Integer> multiplicities, Vector<IFormElement> elements, boolean descend) {
        int i = indexes.size() - 1;
        boolean exitRepeat = false; //if exiting a repetition? (i.e., go to next repetition instead of one level up)

        if (i == -1 || elements.elementAt(i) instanceof GroupDef) {
            // current index is group or repeat or the top-level form

            if (i >= 0) {
                // find out whether we're on a repeat, and if so, whether the
                // specified instance actually exists
                GroupDef group = (GroupDef)elements.elementAt(i);
                if (group.isRepeat()) {
                    if (repeatStructure == REPEAT_STRUCTURE_NON_LINEAR) {
                        if ((multiplicities.lastElement()).intValue() == TreeReference.INDEX_REPEAT_JUNCTURE) {
                            descend = false;
                            exitRepeat = true;
                        }
                    } else {
                        if (form.getMainInstance().resolveReference(form.getChildInstanceRef(elements, multiplicities)) == null) {
                            descend = false; // repeat instance does not exist; do not descend into it
                            exitRepeat = true;
                        }
                    }
                }
            }

            if (descend && (i == -1 || elements.elementAt(i).getChildren().size() > 0)) {
                indexes.addElement(0);
                multiplicities.addElement(0);
                elements.addElement((i == -1 ? form : elements.elementAt(i)).getChild(0));

                if (repeatStructure == REPEAT_STRUCTURE_NON_LINEAR) {
                    if (elements.lastElement() instanceof GroupDef && ((GroupDef)elements.lastElement()).isRepeat()) {
                        multiplicities.setElementAt(TreeReference.INDEX_REPEAT_JUNCTURE, multiplicities.size() - 1);
                    }
                }

                return;
            }
        }

        while (i >= 0) {
            // if on repeat, increment to next repeat EXCEPT when we're on a
            // repeat instance that does not exist and was not created
            // (repeat-not-existing can only happen at lowest level; exitRepeat
            // will be true)
            if (!exitRepeat && elements.elementAt(i) instanceof GroupDef && ((GroupDef)elements.elementAt(i)).isRepeat()) {
                if (repeatStructure == REPEAT_STRUCTURE_NON_LINEAR) {
                    multiplicities.setElementAt(TreeReference.INDEX_REPEAT_JUNCTURE, i);
                } else {
                    multiplicities.setElementAt(multiplicities.elementAt(i) + 1, i);
                }
                return;
            }

            IFormElement parent = (i == 0 ? form : elements.elementAt(i - 1));
            int curIndex = indexes.elementAt(i);

            // increment to the next element on the current level
            if (curIndex + 1 >= parent.getChildren().size()) {
                // at the end of the current level; move up one level and start
                // over
                indexes.removeElementAt(i);
                multiplicities.removeElementAt(i);
                elements.removeElementAt(i);
                i--;
                exitRepeat = false;
            } else {
                indexes.setElementAt(curIndex + 1, i);
                multiplicities.setElementAt(0, i);
                elements.setElementAt(parent.getChild(curIndex + 1), i);

                if (repeatStructure == REPEAT_STRUCTURE_NON_LINEAR) {
                    if (elements.lastElement() instanceof GroupDef && ((GroupDef)elements.lastElement()).isRepeat()) {
                        multiplicities.setElementAt(TreeReference.INDEX_REPEAT_JUNCTURE, multiplicities.size() - 1);
                    }
                }

                return;
            }
        }
    }

    public FormIndex decrementIndex(FormIndex index) {
        Vector<Integer> indexes = new Vector<>();
        Vector<Integer> multiplicities = new Vector<>();
        Vector<IFormElement> elements = new Vector<>();

        if (index.isBeginningOfFormIndex()) {
            return index;
        } else if (index.isEndOfFormIndex()) {
            if (form.getChildren() == null || form.getChildren().size() == 0) {
                return FormIndex.createBeginningOfFormIndex();
            }
        } else {
            form.collapseIndex(index, indexes, multiplicities, elements);
        }

        decrementHelper(indexes, multiplicities, elements);

        if (indexes.size() == 0) {
            return FormIndex.createBeginningOfFormIndex();
        } else {
            return form.buildIndex(indexes, multiplicities, elements);
        }
    }

    private void decrementHelper(Vector<Integer> indexes, Vector<Integer> multiplicities, Vector<IFormElement> elements) {
        int i = indexes.size() - 1;

        if (i != -1) {
            int curIndex = indexes.elementAt(i);
            int curMult = multiplicities.elementAt(i);

            if (repeatStructure == REPEAT_STRUCTURE_NON_LINEAR &&
                    elements.lastElement() instanceof GroupDef && ((GroupDef)elements.lastElement()).isRepeat() &&
                    multiplicities.lastElement().intValue() != TreeReference.INDEX_REPEAT_JUNCTURE) {
                multiplicities.setElementAt(TreeReference.INDEX_REPEAT_JUNCTURE, i);
                return;
            } else if (repeatStructure != REPEAT_STRUCTURE_NON_LINEAR && curMult > 0) {
                multiplicities.setElementAt(curMult - 1, i);
            } else if (curIndex > 0) {
                // set node to previous element
                indexes.setElementAt(curIndex - 1, i);
                multiplicities.setElementAt(0, i);
                elements.setElementAt((i == 0 ? form : elements.elementAt(i - 1)).getChild(curIndex - 1), i);

                if (setRepeatNextMultiplicity(elements, multiplicities))
                    return;
            } else {
                // at absolute beginning of current level; index to parent
                indexes.removeElementAt(i);
                multiplicities.removeElementAt(i);
                elements.removeElementAt(i);
                return;
            }
        }

        IFormElement element = (i < 0 ? form : elements.elementAt(i));
        while (!(element instanceof QuestionDef)) {
            if (element.getChildren().size() == 0) {
                //if there are no children we just return the current index (the group itself)
                return;
            }
            int subIndex = element.getChildren().size() - 1;
            element = element.getChild(subIndex);

            indexes.addElement(subIndex);
            multiplicities.addElement(0);
            elements.addElement(element);

            if (setRepeatNextMultiplicity(elements, multiplicities))
                return;
        }
    }

    private boolean setRepeatNextMultiplicity(Vector<IFormElement> elements, Vector<Integer> multiplicities) {
        // find out if node is repeatable
        TreeReference nodeRef = form.getChildInstanceRef(elements, multiplicities);
        TreeElement node = form.getMainInstance().resolveReference(nodeRef);
        if (node == null || node.isRepeatable()) { // node == null if there are no
            // instances of the repeat
            int mult;
            if (node == null) {
                mult = 0; // no repeats; next is 0
            } else {
                String name = node.getName();
                TreeElement parentNode = form.getMainInstance().resolveReference(nodeRef.getParentRef());
                mult = parentNode.getChildMultiplicity(name);
            }
            multiplicities.setElementAt(new Integer(repeatStructure == REPEAT_STRUCTURE_NON_LINEAR ? TreeReference.INDEX_REPEAT_JUNCTURE : mult), multiplicities.size() - 1);
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method does a recursive check of whether there are any repeat guesses
     * in the element or its subtree. This is a necessary step when initializing
     * the model to be able to identify whether new repeats can be used.
     *
     * @param parent The form element to begin checking
     * @return true if the element or any of its descendants is a repeat
     * which has a count guess, false otherwise.
     */
    private boolean containsRepeatGuesses(IFormElement parent) {
        if (parent instanceof GroupDef) {
            GroupDef g = (GroupDef)parent;
            if (g.isRepeat() && g.getCountReference() != null) {
                return true;
            }
        }

        Vector<IFormElement> children = parent.getChildren();
        if (children == null) {
            return false;
        }
        for (Enumeration en = children.elements(); en.hasMoreElements(); ) {
            if (containsRepeatGuesses((IFormElement)en.nextElement())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieve the serialized debug trace for the element at the specified
     * form index for the provided category of trigger
     *
     * Will enable debugging for the current form (currently doesn't disable
     * afterwards)
     *
     * @param index      The form index to be evaluated
     * @param category   The category of trigger/debug info being requested, like
     *                   calculate, relevant, etc.
     * @param serializer A serializer for the EvaluationTrace
     * @return the output of the provided serializer
     */
    public <T> T getDebugInfo(FormIndex index, String category,
                              EvaluationTraceSerializer<T> serializer) {
        this.getForm().enableDebugTraces();

        Hashtable<String, EvaluationTrace> indexDebug =
                this.getForm().getDebugTraceMap().get(index.getReference());
        if (indexDebug == null || indexDebug.get(category) == null) {
            return null;
        }
        return serializer.serializeEvaluationLevels(indexDebug.get(category));
    }
}
