package org.commcare.resources.model;

public interface InstallCancelled {
    /**
     * @return was the resource install process cancelled by the user or system?
     */
    boolean wasInstallCancelled();
}
