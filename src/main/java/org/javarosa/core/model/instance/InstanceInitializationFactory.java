package org.javarosa.core.model.instance;

/**
 * @author ctsims
 */
public class InstanceInitializationFactory {

    public class InstanceRoot {
        private AbstractTreeElement root;
        private boolean useCaseTemplate;
        public InstanceRoot(AbstractTreeElement root) {
            this(root, false);
        }

        public InstanceRoot(AbstractTreeElement root, boolean useCaseTemplate) {
            this.root = root;
            this.useCaseTemplate = useCaseTemplate;
        }

        public AbstractTreeElement getRoot() {
            return root;
        }

        public boolean useCaseTemplate() {
            return useCaseTemplate;
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
