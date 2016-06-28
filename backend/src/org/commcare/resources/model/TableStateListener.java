package org.commcare.resources.model;

/**
 * @author ctsims
 */
public interface TableStateListener {
    /**
     * A basic resource was added to the table
     */
    void simpleResourceAdded();

    /**
     * A compound resource (i.e. profile or suite) was added to the table.
     * There might now be more resources that need to be processed than before.
     *
     * @param table For calculating updated completed and total resource counts
     */
    void compoundResourceAdded(ResourceTable table);

    void incrementProgress(int complete, int total);
}
