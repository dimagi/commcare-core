package org.commcare.resources.model;

public interface InstallStatsLogger {
    void recordResourceInstallFailure(String resourceName,
                                      Exception errorMsg);
    void recordResourceInstallSuccess(String resourceName);
}
