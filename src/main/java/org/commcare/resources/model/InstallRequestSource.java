package org.commcare.resources.model;

/**
 * All the install workflows a resource install can be part of
 */
public enum InstallRequestSource {
    INSTALL,
    REINSTALL,
    BACKGROUND_UPDATE,
    FOREGROUND_UPDATE,
    RECOVERY,
    BACKGROUND_LAZY_RESOURCE,
    FOREGROUND_LAZY_RESOURCE;

    public boolean isUpdate() {
        return this == BACKGROUND_UPDATE || this == FOREGROUND_UPDATE;
    }
}
