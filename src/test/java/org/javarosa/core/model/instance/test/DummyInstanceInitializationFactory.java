package org.javarosa.core.model.instance.test;

import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.InstanceRoot;

/**
 * Dummy instance initialization factory used in testing.  Doesn't actually
 * support loading external instances, so if it is ever invoked, it raises an
 * error.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class DummyInstanceInitializationFactory extends InstanceInitializationFactory {

    @Override
    public ExternalDataInstance getSpecializedExternalDataInstance(ExternalDataInstance instance) {
        return instance;
    }
    @Override
    public InstanceRoot generateRoot(ExternalDataInstance instance, String locale) {
        throw new RuntimeException("Loading external instances isn't supported " +
                "using this instance initialization factory.");
    }
}
