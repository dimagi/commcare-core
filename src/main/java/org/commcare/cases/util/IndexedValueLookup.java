package org.commcare.cases.util;

/**
 *
 * An indexed value lookup is a singular key/value comparison where the key being checked is
 * indexed by the current platform
 *
 * IE:
 *
 * index = 'a'
 *
 * Created by ctsims on 1/18/2017.
 */

public class IndexedValueLookup implements PredicateProfile {
    public final String key;
    public final Object value;

    public IndexedValueLookup(String key, Object value ) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }
}
