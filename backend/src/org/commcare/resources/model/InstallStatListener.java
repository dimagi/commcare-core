package org.commcare.resources.model;

public interface InstallStatListener {
    void recordResourceInstallFailure(Resource resource,
                                      Exception e);
    void recordResourceInstallSuccess(Resource resource);
}
