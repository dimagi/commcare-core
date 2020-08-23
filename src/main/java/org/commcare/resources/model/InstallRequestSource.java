package org.commcare.resources.model;

/**
 * All the install workflows a resource install can be part of
 */
public enum InstallRequestSource {
    INSTALL,
    BACKGROUND_UPDATE,
    FOREGROUND_UPDATE,
    RECOVERY,
    BACKGROUND_LAZY_RESOURCE,
    FOREGROUND_LAZY_RESOURCE
}
