package org.commcare.util.screen;

import static org.commcare.session.SessionFrame.STATE_MULTIPLE_DATUM_VAL;

import org.commcare.core.interfaces.EntitiesSelectionCache;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.MultiSelectEntityDatum;
import org.javarosa.core.model.instance.TreeReference;

import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Variation of EntityScreen to allow for selection of multiple entities at once
 */
public class MultiSelectEntityScreen extends EntityScreen {

    public static final String USE_SELECTED_VALUES = "use_selected_values";
    private int maxSelectValue = -1;

    private final EntitiesSelectionCache entitiesSelectionCache;
    private UUID storageReferenceId;

    public MultiSelectEntityScreen(boolean handleCaseIndex, boolean full,
            SessionWrapper session,
            EntitiesSelectionCache entitiesSelectionCache)
            throws CommCareSessionException {
        super(handleCaseIndex, full, session);
        this.entitiesSelectionCache = entitiesSelectionCache;
    }

    /**
     * Updates entities selection on the screen
     *
     * @param input          Either 'use_selected_values' or a guid unique to the selection made
     * @param selectedValues Ids of the entities selected on the screen
     * @throws CommCareSessionException
     */
    @Override
    public void updateSelection(String input, @Nullable String[] selectedValues) throws CommCareSessionException {
        setSelectedEntities(input, selectedValues);
    }

    private void setSelectedEntities(String input, @Nullable String[] selectedValues)
            throws CommCareSessionException {
        if (input.contentEquals(USE_SELECTED_VALUES)) {
            processSelectedValues(selectedValues);
        } else {
            UUID inputId = UUID.fromString(input);
            String[] cachedSelection = entitiesSelectionCache.read(inputId);
            if (cachedSelection == null) {
                throw new CommCareSessionException(
                        "Could not make selection with reference id " + input + " on this screen. " +
                                " If this error persists please report a bug to CommCareHQ.");
            }
            storageReferenceId = inputId;
        }
    }

    private void processSelectedValues(String[] selectedValues)
            throws CommCareSessionException {
        if (selectedValues != null) {
            String[] evaluatedValues = new String[selectedValues.length];
            for (int i = 0; i < selectedValues.length; i++) {
                TreeReference currentReference = getEntityReference(selectedValues[i]);
                if (currentReference == null) {
                    throw new CommCareSessionException(
                            "Could not select case " + selectedValues[i] + " on this screen. " +
                                    " If this error persists please report a bug to CommCareHQ.");
                }
                evaluatedValues[i] = getReturnValueFromSelection(currentReference);
            }

            UUID guid = entitiesSelectionCache.write(evaluatedValues);
            storageReferenceId = guid;
        }
    }

    @Override
    protected void setSession(SessionWrapper session) throws CommCareSessionException {
        super.setSession(session);
        maxSelectValue = ((MultiSelectEntityDatum)mNeededDatum).getMaxSelectValue();
    }

    @Override
    protected void updateSession(CommCareSession session) {
        if (executePendingAction(session)) {
            return;
        }
        if (storageReferenceId != null) {
            session.setDatum(STATE_MULTIPLE_DATUM_VAL, mNeededDatum.getDataId(), storageReferenceId.toString());
        }
    }

    @Override
    public boolean referencesContainStep(String stepValue) {
        return entitiesSelectionCache.contains(UUID.fromString(stepValue));
    }

    public int getMaxSelectValue() {
        return maxSelectValue;
    }

    public UUID getStorageReferenceId() {
        return storageReferenceId;
    }
}
