package org.javarosa.core.model.instance;

/**
 * Allows manual exceptions triggering; useful for testing failure modes.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class FormInstanceWithFailures extends FormInstance {
    private static boolean failOnIdSet = false;

    public FormInstanceWithFailures(TreeElement root) {
        super(root);
    }

    /**
     * Toggle failing setID behavior; useful for testing atomic database actions
     */
    public static void setFailOnIdSet(boolean shouldFail) {
        failOnIdSet = shouldFail;
    }

    @Override
    public void setID(int id) {
        if (failOnIdSet) {
            throw new RuntimeException("");
        } else {
            super.setID(id);
        }
    }
}
