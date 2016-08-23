package org.commcare.util;

/**
 * Define where to get localized array values from
 */

public interface ArrayDataSource {
    String[] getArray(String key);
}
