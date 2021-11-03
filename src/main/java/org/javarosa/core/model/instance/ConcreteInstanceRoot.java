package org.javarosa.core.model.instance;

/**
 * Wrapper class for instances that do not require additional metadata.
 * This applies to instances which are pre-defined by the platform such as
 * the `commcaresession` instance.
 */
public class ConcreteInstanceRoot implements InstanceRoot {
    protected AbstractTreeElement root;

    public ConcreteInstanceRoot(AbstractTreeElement root) {
        this.root = root;
    }

    public AbstractTreeElement getRoot() {
        return root;
    }

    public void setupNewCopy(ExternalDataInstance instance) {
        instance.copyFromSource(this);
    }
}
