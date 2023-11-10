package org.commcare.cases.query;

/**
 *
 * A negative indexed value lookup is a singular key/value comparison where the key being checked is
 * indexed by the current platform
 *
 * IE:
 *
 * index != 'a'
 *
 * Created by cellowitz on 11/23/2020.
 */

public class NegativeIndexedValueLookup implements PredicateProfile {
    public final String key;
    public final Object value;

    public NegativeIndexedValueLookup(String key, Object value ) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "NegativeIndexedValueLookup{" +
                "key='" + key + '\'' +
                ", value=" + value +
                '}';
    }
}
