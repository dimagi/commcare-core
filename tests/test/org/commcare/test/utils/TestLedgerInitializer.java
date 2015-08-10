package org.commcare.test.utils;

import org.commcare.cases.ledger.instance.LedgerInstanceTreeElement;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;

/**
 * Initializing a ledger from from sandboxed storage
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class TestLedgerInitializer extends InstanceInitializationFactory {
    private final MockUserDataSandbox sandbox;

    public TestLedgerInitializer(MockUserDataSandbox sandbox) {
        this.sandbox = sandbox;
    }

    public AbstractTreeElement generateRoot(ExternalDataInstance instance) {
        String ref = instance.getReference();
        if (ref.contains(LedgerInstanceTreeElement.MODEL_NAME)) {
            return new LedgerInstanceTreeElement(instance.getBase(), sandbox.getLedgerStorage());
        }
        return null;
    }
}
