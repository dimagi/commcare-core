package org.commcare.util.screen;

import static org.commcare.session.SessionFrame.STATE_MULTIPLE_DATUM_VAL;

import org.commcare.core.interfaces.EntitiesSelectionCache;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.MultiSelectEntityDatum;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.PropertyUtils;

import java.sql.SQLException;

import javax.annotation.Nullable;

/**
 * Variation of EntityScreen to allow for selection of multiple entities at once
 */
public class MultiSelectEntityScreen extends EntityScreen {

    private int maxSelectValue = -1;

    private final EntitiesSelectionCache entitiesSelectionCache;

    public MultiSelectEntityScreen(boolean handleCaseIndex, boolean full,
            SessionWrapper session, @Nullable String[] selectedValues,
            EntitiesSelectionCache entitiesSelectionCache)
            throws CommCareSessionException, SQLException {
        super(handleCaseIndex, full, session);
        this.entitiesSelectionCache = entitiesSelectionCache;
        processSelectedValues(selectedValues, session);
    }

    private void processSelectedValues(String[] selectedValues, SessionWrapper session)
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
            session.setDatum(STATE_MULTIPLE_DATUM_VAL, mNeededDatum.getDataId(), guid);
        }
    }

    @Override
    protected void setSession(SessionWrapper session) throws CommCareSessionException {
        super.setSession(session);
        maxSelectValue = ((MultiSelectEntityDatum)mNeededDatum).getMaxSelectValue();
    }

    @Override
    public void setHighlightedEntity(String id) throws CommCareSessionException {
        String[] cachedSelction = entitiesSelectionCache.read(id);
        if (cachedSelction == null) {
            throw new CommCareSessionException(
                    "Could not make selection with reference id " + id + " on this screen. " +
                            " If this error persists please report a bug to CommCareHQ.");
        }
    }

    @Override
    protected void updateSession(CommCareSession session) {
        if (executePendingAction(session)) {
            return;
        }
    }

    public int getMaxSelectValue() {
        return maxSelectValue;
    }

}
