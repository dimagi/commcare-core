package org.commcare.resources.model;

public enum InstallRequestSource {
    BACKGROUND_UPDATE,
    FOREGROUND_UPDATE,
    RECOVERY,
    BACKGROUND_LAZY_RESOURCE,
    FOREGROUND_LAZY_RESOURCE
}
