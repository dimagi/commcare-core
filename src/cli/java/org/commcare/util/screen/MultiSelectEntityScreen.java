package org.commcare.util.screen;

import static org.commcare.session.SessionFrame.STATE_MULTIPLE_DATUM_VAL;

import org.commcare.core.interfaces.EntitiesSelectionCache;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.MultiSelectEntityDatum;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.PropertyUtils;

import java.sql.SQLException;

/**
 * Variation of EntityScreen to allow for selection of multiple entities at once
 */
public class MultiSelectEntityScreen extends EntityScreen {

    public static final String USE_SELECTED_VALUES = "use_selected_values";
    private int maxSelectValue = -1;

    private final EntitiesSelectionCache entitiesSelectionCache;
    private String storageReferenceId;

    public MultiSelectEntityScreen(boolean handleCaseIndex, boolean full,
            SessionWrapper session,
            EntitiesSelectionCache entitiesSelectionCache)
            throws CommCareSessionException {
        super(handleCaseIndex, full, session);
        this.entitiesSelectionCache = entitiesSelectionCache;
    }

    public void setSelectedEntities(String input, String[] selectedValues)
            throws CommCareSessionException {
        try {
            if (input.contentEquals(USE_SELECTED_VALUES)) {
                processSelectedValues(selectedValues);
            } else {
                String[] cachedSelction = entitiesSelectionCache.read(input);
                if (cachedSelction == null) {
                    throw new CommCareSessionException(
                            "Could not make selection with reference id " + input + " on this screen. " +
                                    " If this error persists please report a bug to CommCareHQ.");
                }
                storageReferenceId = input;
            }
        } catch (SQLException throwables) {
            throw new CommCareSessionException(
                    "An error occurred trying to process selections on this screen. " +
                            " If this error persists please report a bug to CommCareHQ.", throwables);
        }
    }

    private void processSelectedValues(String[] selectedValues)
            throws SQLException, CommCareSessionException {
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

            String guid = PropertyUtils.genGUID(10);
            entitiesSelectionCache.cache(guid, evaluatedValues);
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
            session.setDatum(STATE_MULTIPLE_DATUM_VAL, mNeededDatum.getDataId(), storageReferenceId);
        }
    }

    @Override
    public boolean referencesContainStep(String stepValue) {
        try {
            String[] cachedSelection = entitiesSelectionCache.read(stepValue);
            return cachedSelection != null;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    public int getMaxSelectValue() {
        return maxSelectValue;
    }

    public String getStorageReferenceId() {
        return storageReferenceId;
    }
}
