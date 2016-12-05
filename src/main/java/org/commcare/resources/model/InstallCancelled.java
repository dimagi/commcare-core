package org.commcare.resources.model;

// TODO PLM: Have installers that download files check cancellation on big downloads
public interface InstallCancelled {
    /**
     * @return was the resource install process cancelled by the user or system?
     */
    boolean wasInstallCancelled();
}
