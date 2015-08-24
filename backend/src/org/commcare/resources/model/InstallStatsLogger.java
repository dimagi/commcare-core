package org.commcare.resources.model;

public interface InstallStatsLogger {
    void recordResourceInstallFailure(String resourceName,
                                      String errorMsg);
    void recordResourceInstallSuccess(String resourceName);
}
