package org.commcare.test.utilities;

import org.commcare.cases.instance.CaseDataInstance;
import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.instance.ConcreteInstanceRoot;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.InstanceRoot;

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

    @Override
    public ExternalDataInstance getSpecializedExternalDataInstance(ExternalDataInstance instance) {
        if (CaseInstanceTreeElement.MODEL_NAME.equals(instance.getInstanceId())) {
            return new CaseDataInstance(instance);
        } else {
            return instance;
        }
    }

    @Override
    public InstanceRoot generateRoot(ExternalDataInstance instance) {
        String ref = instance.getReference();
        if (ref.contains(CaseInstanceTreeElement.MODEL_NAME)) {
            CaseInstanceTreeElement root = new CaseInstanceTreeElement(instance.getBase(), sandbox.getCaseStorage());
            return new ConcreteInstanceRoot(root);
        }
        return ConcreteInstanceRoot.NULL;
    }
}
