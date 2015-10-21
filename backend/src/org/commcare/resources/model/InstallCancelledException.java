package org.commcare.resources.model;

/**
 * Exception signifying that the user or system cancelled a running update/install
 */
public class InstallCancelledException extends Exception {
    public InstallCancelledException(String message) {
        super(message);
    }
}
