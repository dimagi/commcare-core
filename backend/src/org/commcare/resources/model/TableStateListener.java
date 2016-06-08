package org.commcare.resources.model;

/**
 * @author ctsims
 */
public interface TableStateListener {
    void resourceStateIncremented();

    void resourceStateUpdated(ResourceTable table);

    void incrementProgress(int complete, int total);
}
