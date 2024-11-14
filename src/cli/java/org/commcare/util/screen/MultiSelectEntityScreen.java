package org.commcare.util.screen;

import static org.commcare.session.SessionFrame.STATE_MULTIPLE_DATUM_VAL;
import static org.commcare.xml.SessionDatumParser.DEFAULT_MAX_SELECT_VAL;

import com.google.common.collect.ImmutableMap;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.interfaces.VirtualDataInstanceStorage;
import org.commcare.data.xml.VirtualInstances;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.MultiSelectEntityDatum;
import org.commcare.util.FormDataUtil;
import org.commcare.util.exception.InvalidEntitiesSelectionException;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Variation of EntityScreen to allow for selection of multiple entities at once
 */
public class MultiSelectEntityScreen extends EntityScreen {

    public static final String USE_SELECTED_VALUES = "use_selected_values";
    private int maxSelectValue = DEFAULT_MAX_SELECT_VAL;

    private final VirtualDataInstanceStorage virtualDataInstanceStorage;
    private String storageReferenceId;
    private ExternalDataInstance selectedValuesInstance;

    public MultiSelectEntityScreen(boolean handleCaseIndex, boolean needsFullInit,
            SessionWrapper session,
            VirtualDataInstanceStorage virtualDataInstanceStorage,
            EntityScreenContext entityScreenContext)
            throws CommCareSessionException {
        super(handleCaseIndex, needsFullInit, session, entityScreenContext);
        this.virtualDataInstanceStorage = virtualDataInstanceStorage;
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

    @Override
    public void updateSelection(String input, TreeReference[] selectedRefs)
            throws CommCareSessionException {
        if (input.contentEquals(USE_SELECTED_VALUES)) {
            processSelectedReferences(selectedRefs);
        } else {
            prcessSelectionAsGuid(input);
        }
    }

    @Override
    protected boolean shouldAutoSelect() {
        return mNeededDatum.isAutoSelectEnabled() && references.size() != 0;
    }

    @Override
    public boolean autoSelectEntities(SessionWrapper session) {
        int selectionSize = references.size();
        if (validateSelectionSize(selectionSize)) {
            String[] evaluatedValues = new String[selectionSize];
            for (int i = 0; i < selectionSize; i++) {
                evaluatedValues[i] = getReturnValueFromSelection(references.elementAt(i));
            }
            processSelectionIntoInstance(evaluatedValues, getNeededDatumId());
            updateSession(session);
            return true;
        }
        return false;
    }

    private boolean validateSelectionSize(int selectionSize) {
        if (selectionSize == 0) {
            return false;
        } else if (selectionSize > maxSelectValue) {
            throw new InvalidEntitiesSelectionException(getMaxSelectError(selectionSize));
        }
        return true;
    }

    private String getMaxSelectError(int selectionSize) {
        String error;
        try {
            if (maxSelectValue == 1) {
                error = Localization.get("case.list.max.selection.error.singular",
                        new String[]{String.valueOf(selectionSize)});
            } else {
                error = Localization.get("case.list.max.selection.error",
                        new String[]{String.valueOf(selectionSize), String.valueOf(maxSelectValue)});
            }
        } catch (NoLocalizedTextException | NullPointerException e) {
            if (maxSelectValue == 1) {
                error = String.format("Too many cases(%d) to proceed. Only 1 is allowed", selectionSize);
            } else {
                error = String.format("Too many cases(%d) to proceed. Only %d are allowed",
                        selectionSize, maxSelectValue);
            }
        }
        return error;
    }

    private void setSelectedEntities(String input, @Nullable String[] selectedValues)
            throws CommCareSessionException {
        if (input.contentEquals(USE_SELECTED_VALUES)) {
            processSelectedValues(selectedValues);
        } else {
            prcessSelectionAsGuid(input);
        }
    }

    private void prcessSelectionAsGuid(String guid) throws CommCareSessionException {
        String datumId = getNeededDatumId();
        ExternalDataInstance cachedInstance = virtualDataInstanceStorage.read(guid, datumId, datumId);
        if (cachedInstance == null) {
            throw new CommCareSessionException(
                    "Could not make selection with reference id " + guid + " on this screen. " +
                            " If this error persists please report a bug to CommCareHQ.");
        }
        validateEntitiesInInstance(cachedInstance);
        storageReferenceId = guid;
    }

    private void validateEntitiesInInstance(ExternalDataInstance instance) throws CommCareSessionException {
        AbstractTreeElement root = instance.getRoot();
        for (int i = 0; i < root.getNumChildren(); i++) {
            String entityVal = root.getChildAt(i).getValue().uncast().getString();
            getAndValidateEntityReference(entityVal);
        }
    }

    private String getNeededDatumId() {
        return getSession().getNeededDatum().getDataId();
    }

    private void processSelectedReferences(TreeReference[] selectedRefs) {
        if (validateSelectionSize(selectedRefs.length)) {
            String[] evaluatedValues = new String[selectedRefs.length];
            for (int i = 0; i < selectedRefs.length; i++) {
                evaluatedValues[i] = getReturnValueFromSelection(selectedRefs[i]);
            }
            processSelectionIntoInstance(evaluatedValues, getNeededDatumId());
        }
    }

    private void processSelectedValues(String[] selectedValues)
            throws CommCareSessionException {
        if (selectedValues != null && validateSelectionSize(selectedValues.length)) {
            String[] evaluatedValues = new String[selectedValues.length];
            for (int i = 0; i < selectedValues.length; i++) {
                TreeReference currentReference = getAndValidateEntityReference(selectedValues[i]);
                evaluatedValues[i] = getReturnValueFromSelection(currentReference);
            }
            processSelectionIntoInstance(evaluatedValues, getNeededDatumId());
        }
    }

    private TreeReference getAndValidateEntityReference(String selectedValue) throws CommCareSessionException {
        TreeReference currentReference = getEntityReference(selectedValue);
        if (currentReference == null) {
            throw new CommCareSessionException(
                    "Could not select case " + selectedValue + " on this screen. " +
                            " If this error persists please report a bug to CommCareHQ.");
        }
        return currentReference;
    }

    private void processSelectionIntoInstance(String[] evaluatedValues, String instanceId) {
        Pair<String, ExternalDataInstance> guidAndInstance = VirtualInstances.storeSelectedValuesInInstance(
                virtualDataInstanceStorage, evaluatedValues, instanceId);
        storageReferenceId = guidAndInstance.first;
        selectedValuesInstance = guidAndInstance.second;
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
            if (selectedValuesInstance == null) {
                String datumId = getNeededDatumId();
                selectedValuesInstance = virtualDataInstanceStorage.read(storageReferenceId, datumId, datumId);
            }
            ExternalDataInstanceSource externalDataInstanceSource = ExternalDataInstanceSource.buildVirtual(
                    selectedValuesInstance, storageReferenceId);
            session.setDatum(STATE_MULTIPLE_DATUM_VAL, mNeededDatum.getDataId(),
                    storageReferenceId, externalDataInstanceSource);
        }
    }

    @Override
    public void updateDatum(CommCareSession session, String input) {
        storageReferenceId = input;
        String dataId = getNeededDatumId();
        selectedValuesInstance = virtualDataInstanceStorage.read(storageReferenceId, dataId, dataId);
        ExternalDataInstanceSource externalDataInstanceSource = ExternalDataInstanceSource.buildVirtual(
                selectedValuesInstance, storageReferenceId);
        session.setDatum(STATE_MULTIPLE_DATUM_VAL, dataId, input, externalDataInstanceSource);
    }

    @Override
    public String getBreadcrumb(String input, UserSandbox sandbox, SessionWrapper session) {
        if (selectedValuesInstance != null) {
            AbstractTreeElement root = selectedValuesInstance.getRoot();
            int caseCount = root.getNumChildren();
            if (caseCount > 0) {
                String caseId = root.getChildAt(0).getValue().getDisplayText();
                String caseName = FormDataUtil.getCaseName(sandbox, caseId);
                if (caseName != null) {
                    if (caseCount > 1) {
                        return "(" + caseCount + ") " + caseName + ", ...";
                    } else {
                        return caseName;
                    }
                }
            }
        }
        return ScreenUtils.getBestTitle(session);
    }

    @Nonnull
    @Override
    protected EvaluationContext getAutoLaunchEvaluationContext(String nextInput) {
        ExternalDataInstance instance;
        if (referencesContainStep(nextInput)) {
            instance = virtualDataInstanceStorage.read(nextInput, "next_input", getNeededDatumId());
        } else {
            // empty instance
            instance = VirtualInstances.buildSelectedValuesInstance("next_input", new String[0]);
        }
        return getEvalContext().spawnWithCleanLifecycle(ImmutableMap.of(
                "next_input", instance
        ));
    }

    @Override
    public boolean referencesContainStep(String stepValue) {
        return virtualDataInstanceStorage.contains(stepValue);
    }

    public int getMaxSelectValue() {
        return maxSelectValue;
    }

    public String getStorageReferenceId() {
        return storageReferenceId;
    }

    @Override
    public boolean handleInputAndUpdateSession(CommCareSession session, String input,
            boolean allowAutoLaunch, String[] selectedValues, boolean respectRelevancy) throws CommCareSessionException {
        super.handleInputAndUpdateSession(session, input, allowAutoLaunch, selectedValues, respectRelevancy);
        return false;
    }

    @Override
    public String toString() {
        return "MultiSelectEntityScreen [id=" + mNeededDatum.getDataId() + ", selection=" + storageReferenceId + "]";
    }
}
