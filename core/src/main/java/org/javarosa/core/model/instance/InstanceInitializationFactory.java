package org.javarosa.core.model.instance;

/**
 * @author ctsims
 */
public interface InstanceInitializationFactory {
    /**
     * Specializes the instance to an ExternalDataInstance class extension.
     * E.g. one might want to use the CaseDataInstance if the instanceId is
     * "casedb"
     */
    ExternalDataInstance getSpecializedExternalDataInstance(ExternalDataInstance instance);

    AbstractTreeElement generateRoot(ExternalDataInstance instance);
}
