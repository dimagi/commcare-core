package org.javarosa.core.model.instance;

/**
 * @author ctsims
 */
public class InstanceInitializationFactory {

    public interface InstanceRoot {
        AbstractTreeElement getRoot();
        void setupNewCopy(ExternalDataInstance instance);
    }

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
    /**
     * Specializes the instance to an ExternalDataInstance class extension.
     * E.g. one might want to use the CaseDataInstance if the instanceId is
     * "casedb"
     */
    public ExternalDataInstance getSpecializedExternalDataInstance(ExternalDataInstance instance) {
        return instance;
    }

    public InstanceRoot generateRoot(ExternalDataInstance instance) {
        return null;
    }
}
