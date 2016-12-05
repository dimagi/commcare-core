package org.commcare.resources.model;

/**
 * Register resource table installation success and failure stats.
 */
public interface InstallStatsLogger {
    void recordResourceInstallFailure(String resourceName,
                                      Exception errorMsg);
    void recordResourceInstallSuccess(String resourceName);
}
