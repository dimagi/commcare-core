package org.commcare.test.utils;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;

/**
 * Utility class for initializing abstract data instances from
 * sandboxed storage
 *
 * @author ctsims
 */
public class TestInstanceInitializer extends InstanceInitializationFactory {
    private final MockUserDataSandbox sandbox;

    public TestInstanceInitializer(MockUserDataSandbox sandbox) {
        this.sandbox = sandbox;
    }

    public AbstractTreeElement generateRoot(ExternalDataInstance instance) {
        String ref = instance.getReference();
        if (ref.contains(CaseInstanceTreeElement.MODEL_NAME)) {
            return new CaseInstanceTreeElement(instance.getBase(), sandbox.getCaseStorage(), false);
        }
        return null;
    }
}
