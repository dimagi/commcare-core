package org.commcare.util.screen;

import static org.commcare.session.SessionFrame.STATE_MULTIPLE_DATUM_VAL;

import com.google.common.collect.ImmutableMap;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.interfaces.VirtualDataInstanceStorage;
import org.commcare.data.xml.VirtualInstances;
import org.commcare.modern.session.SessionWrapper;
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
    private static final int DEFAULT_MAX_SELECT_VAL = 100;
    private int maxSelectValue = DEFAULT_MAX_SELECT_VAL;

    private final VirtualDataInstanceStorage virtualDataInstanceStorage;
    private String storageReferenceId;
    private ExternalDataInstance selectedValuesInstance;

    public MultiSelectEntityScreen(boolean handleCaseIndex, boolean needsFullInit,
            SessionWrapper session,
            VirtualDataInstanceStorage virtualDataInstanceStorage,
            boolean isDetailScreen)
            throws CommCareSessionException {
        super(handleCaseIndex, needsFullInit, session, isDetailScreen);
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
        return mNeededDatum.isAutoSelectEnabled();
    }

    @Override
    public void autoSelectEntities(SessionWrapper session) {
        int selectionSize = references.size();
        if (validateSelectionSize(selectionSize)) {
            String[] evaluatedValues = new String[selectionSize];
            for (int i = 0; i < selectionSize; i++) {
                evaluatedValues[i] = getReturnValueFromSelection(references.elementAt(i));
            }
            processSelectionIntoInstance(evaluatedValues);
        }
        updateSession(session);
    }

    private boolean validateSelectionSize(int selectionSize) {
        if (selectionSize == 0) {
            throw new InvalidEntitiesSelectionException(getNoEntitiesError());
        } else if (selectionSize > maxSelectValue) {
            throw new InvalidEntitiesSelectionException(getMaxSelectError(selectionSize));
        }
        return true;
    }

    private String getMaxSelectError(int selectionSize) {
        try {
            return Localization.get("case.list.max.selection.error",
                    new String[]{String.valueOf(selectionSize), String.valueOf(maxSelectValue)});
        } catch (NoLocalizedTextException | NullPointerException e) {
            return String.format("Number of selected cases %d is greater than the maximum limit of %d",
                    selectionSize, maxSelectValue);
        }
    }

    private String getNoEntitiesError() {
        try {
            return Localization.get("case.list.no.selection.error");
        } catch (NoLocalizedTextException | NullPointerException e) {
            return String.format("No cases found");
        }
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
        ExternalDataInstance cachedInstance = virtualDataInstanceStorage.read(
                guid, getSession().getNeededDatum().getDataId());
        if (cachedInstance == null) {
            throw new CommCareSessionException(
                    "Could not make selection with reference id " + guid + " on this screen. " +
                            " If this error persists please report a bug to CommCareHQ.");
        }
        storageReferenceId = guid;
    }

    private void processSelectedReferences(TreeReference[] selectedRefs) {
        if (validateSelectionSize(selectedRefs.length)) {
            String[] evaluatedValues = new String[selectedRefs.length];
            for (int i = 0; i < selectedRefs.length; i++) {
                evaluatedValues[i] = getReturnValueFromSelection(selectedRefs[i]);
            }
            processSelectionIntoInstance(evaluatedValues);
        }
    }

    private void processSelectedValues(String[] selectedValues)
            throws CommCareSessionException {
        if (selectedValues != null && validateSelectionSize(selectedValues.length)) {
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
            processSelectionIntoInstance(evaluatedValues);
        }
    }

    private void processSelectionIntoInstance(String[] evaluatedValues) {
        ExternalDataInstance instance = VirtualInstances.buildSelectedValuesInstance(
                getSession().getNeededDatum().getDataId(),
                evaluatedValues);
        String guid = virtualDataInstanceStorage.write(instance);
        storageReferenceId = guid;

        // rebuild instance with the source
        ExternalDataInstanceSource instanceSource = ExternalDataInstanceSource.buildVirtual(instance,
                storageReferenceId);
        selectedValuesInstance = instanceSource.toInstance();
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
                selectedValuesInstance = virtualDataInstanceStorage.read(
                        storageReferenceId, getSession().getNeededDatum().getDataId());
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
        String dataId = session.getNeededDatum().getDataId();
        selectedValuesInstance = virtualDataInstanceStorage.read(storageReferenceId, dataId);
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
            instance = virtualDataInstanceStorage.read(nextInput, "next_input");
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
            boolean allowAutoLaunch, String[] selectedValues) throws CommCareSessionException {
        super.handleInputAndUpdateSession(session, input, allowAutoLaunch, selectedValues);
        return false;
    }

    @Override
    public String toString() {
        return "MultiSelectEntityScreen [id=" + mNeededDatum.getDataId() + ", selection=" + storageReferenceId + "]";
    }
}
