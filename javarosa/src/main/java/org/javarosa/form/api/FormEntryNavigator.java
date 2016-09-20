package org.javarosa.form.api;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;

/**
 * Created by willpride on 9/20/16.
 */
public class FormEntryNavigator {

    FormEntryController mFormEntryController;
    // TODO implement read only
    boolean mReadOnly = false;

    public FormEntryNavigator (FormEntryController formEntryController) {
        this.mFormEntryController = formEntryController;
    }

    public FormIndex getNextFormIndex(FormIndex index, boolean stepOverGroup) {
        return getNextFormIndex(index, stepOverGroup, true);
    }

    /**
     * Get the FormIndex after the given one.
     */
    public FormIndex getNextFormIndex(FormIndex index, boolean stepOverGroup, boolean expandRepeats) {
        //TODO: this won't actually catch the case where there are nested field lists properly
        if (mFormEntryController.getModel().getEvent(index) == FormEntryController.EVENT_GROUP && indexIsInFieldList(index) && stepOverGroup) {
            return getIndexPastGroup(index);
        } else {
            index = mFormEntryController.getNextIndex(index, expandRepeats);
            if (mFormEntryController.getModel().getEvent(index) == FormEntryController.EVENT_PROMPT_NEW_REPEAT && this.mReadOnly) {
                return getNextFormIndex(index, stepOverGroup, expandRepeats);
            }
            return index;
        }
    }

    /**
     * Navigates backward in the form.
     *
     * @return the event that should be handled by a view.
     */
    public FormIndex getPreviousFormIndex() {
        /*
         * Right now this will always skip to the beginning of a group if that group is represented
         * as a 'field-list'. Should a need ever arise to step backwards by only one step in a
         * 'field-list', this method will have to be updated.
         */

        int event = mFormEntryController.stepToPreviousEvent();

        if (event == FormEntryController.EVENT_PROMPT_NEW_REPEAT &&
                this.mReadOnly) {
            getPreviousFormIndex();
        }


        // If after we've stepped, we're in a field-list, jump back to the beginning of the group
        FormIndex host = getFieldListHost(this.getFormIndex());
        if (host != null) {
            mFormEntryController.jumpToIndex(host);
        }
        return getFormIndex();
    }

    /**
     * From the given FormIndex which must be a group element,
     * find the next index which is outside of that group.
     *
     * @return FormIndex
     */
    private FormIndex getIndexPastGroup(FormIndex index) {
        // Walk until the next index is outside of this one.
        FormIndex walker = index;
        while (FormIndex.isSubElement(index, walker)) {
            walker = getNextFormIndex(walker, false);
        }
        return walker;
    }

    /**
     * Tests if the FormIndex 'index' is located inside a group that is marked as a "field-list"
     *
     * @return true if index is in a "field-list". False otherwise.
     */
    private boolean indexIsInFieldList(FormIndex index) {
        FormIndex fieldListHost = this.getFieldListHost(index);
        return fieldListHost != null;
    }

    /**
     * Tests if the current FormIndex is located inside a group that is marked as a "field-list"
     *
     * @return true if index is in a "field-list". False otherwise.
     */
    public boolean indexIsInFieldList() {
        return indexIsInFieldList(mFormEntryController.getModel().getFormIndex());
    }


    /**
     * Retrieves the index of the Group that is the host of a given field list.
     */
    private FormIndex getFieldListHost(FormIndex child) {
        int event = mFormEntryController.getModel().getEvent(child);

        if (event == FormEntryController.EVENT_QUESTION || event == FormEntryController.EVENT_GROUP || event == FormEntryController.EVENT_REPEAT) {
            // caption[0..len-1]
            // caption[len-1] == the event itself
            // caption[len-2] == the groups containing this group
            FormEntryCaption[] captions = mFormEntryController.getModel().getCaptionHierarchy();

            //This starts at the beginning of the heirarchy, so it'll catch the top-level
            //host index.
            for (FormEntryCaption caption : captions) {
                FormIndex parentIndex = caption.getIndex();
                if (isFieldListHost(parentIndex)) {
                    return parentIndex;
                }
            }

            //none of this node's parents are field lists
            return null;

        } else {
            // Non-host elements can't have field list hosts.
            return null;
        }
    }

    /**
     * A convenience method for determining if the current FormIndex is a group that is/should be
     * displayed as a multi-question view of all of its descendants. This is useful for returning
     * from the formhierarchy view to a selected index.
     */
    private boolean isFieldListHost(FormIndex index) {
        // if this isn't a group, return right away
        if (!(mFormEntryController.getModel().getForm().getChild(index) instanceof GroupDef)) {
            return false;
        }

        //TODO: Is it possible we need to make sure this group isn't inside of another group which
        //is itself a field list? That would make the top group the field list host, not the
        //descendant group

        GroupDef gd = (GroupDef)mFormEntryController.getModel().getForm().getChild(index); // exceptions?
        return ("field-list".equalsIgnoreCase(gd.getAppearanceAttr()));
    }

    /**
     * @return current FormIndex.
     */
    public FormIndex getFormIndex() {
        return mFormEntryController.getModel().getFormIndex();
    }

}
