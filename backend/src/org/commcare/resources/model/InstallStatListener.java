package org.commcare.resources.model;

public interface InstallStatListener {
    void recordResourceInstallFailure(String resourceName,
                                      String errorMsg);
    void recordResourceInstallSuccess(String resourceName);
}
