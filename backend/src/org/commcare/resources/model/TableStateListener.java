/**
 *
 */
package org.commcare.resources.model;

/**
 * @author ctsims
 */
public interface TableStateListener {
    public void resourceStateUpdated(ResourceTable table);

    public void incrementProgress(int complete, int total);
}
