package org.commcare.resources.model;

public interface InstallStatListener {
    void recordResourceInstallFailure(String resourceName,
                                      Exception e);
    void recordResourceInstallSuccess(String resourceName);
}
