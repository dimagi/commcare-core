package org.javarosa.core.model.instance;

import javax.annotation.Nonnull;

/**
 * @author ctsims
 */
public class InstanceInitializationFactory {

    /**
     * Specializes the instance to an ExternalDataInstance class extension.
     * E.g. one might want to use the CaseDataInstance if the instanceId is
     * "casedb"
     */
    public ExternalDataInstance getSpecializedExternalDataInstance(ExternalDataInstance instance) {
        return instance;
    }

    @Nonnull
    public InstanceRoot generateRoot(ExternalDataInstance instance, String locale) {
        return ConcreteInstanceRoot.NULL;
    }
}
