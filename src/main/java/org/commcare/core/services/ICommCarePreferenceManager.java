package org.commcare.core.services;

/**
 * Interface to be implemented by any custom preference implementation. Add any putXXX getXXX methods as you need them.
 */
public interface ICommCarePreferenceManager {

    void putLong(String key, long value);

    long getLong(String key, long defaultValue);
}
