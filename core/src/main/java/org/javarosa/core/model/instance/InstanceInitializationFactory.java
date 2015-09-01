package org.javarosa.core.model.instance;

/**
 * @author ctsims
 */
public abstract class InstanceInitializationFactory {
    public abstract DataInstance getSpecializedInstance(ExternalDataInstance instance);
    public abstract AbstractTreeElement generateRoot(ExternalDataInstance instance);
}
