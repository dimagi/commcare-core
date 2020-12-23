package org.commcare.resources;

import org.commcare.resources.model.InstallRequestSource;

/**
 * Defines context for Resource installation requests
 */
public class ResourceInstallContext {

    private InstallRequestSource mInstallRequestSource;

    public ResourceInstallContext(InstallRequestSource installRequestSource) {
        mInstallRequestSource = installRequestSource;
    }

    public InstallRequestSource getInstallRequestSource() {
        return mInstallRequestSource;
    }
}
