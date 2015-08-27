package org.javarosa.core.model.instance;

/**
 * Provides generic way to build objects that extend ExternalDataInstance.
 * Allows building custom external data instances that have been injected by
 * projects that depend on JavaRosa. E.g.: CommCare's CaseDataInstance
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public interface ExternalDataInstanceBuilder<T extends ExternalDataInstance> {
    /**
     * @param dataReference Resolves to Where the instance's data is stored.
     *                      E.g. jr://instance/casedb
     * @param instanceId    How the instance will be accessed via xform
     *                      "instance('instanceId')"
     * @return new instance of a class that extends ExternalDataInstance
     */
    T buildExternalDataInstance(String dataReference, String instanceId);
}
