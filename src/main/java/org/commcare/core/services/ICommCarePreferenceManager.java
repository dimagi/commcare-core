package org.commcare.core.services;

/**
 * Interface to be implemented by any custom preference implementation. Add any putXXX getXXX methods as you need them.
 */
public interface ICommCarePreferenceManager {

    void putLong(String key, long value);

    long getLong(String key, long defaultValue);

    void putBoolean(String key, boolean value);

    boolean getBoolean(String key, boolean defaultValue);
}
