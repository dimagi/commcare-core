package org.commcare.util;

import org.javarosa.core.model.instance.FormInstance;

/**
 * A functional wrapper for the form processing workflow.
 *
 * See CommCareFormEntryState
 *
 * @author ctsims
 *
 */
public interface FormTransportWorkflow {
    public void preProcessing(FormInstance instance);
    public void postProcessing();
}
