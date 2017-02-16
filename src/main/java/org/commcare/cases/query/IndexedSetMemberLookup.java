package org.commcare.cases.query;

/**
 * An indexed set member lookup is a check for whether a value which is indexed on the current
 * platform is a member of a set of elements.
 *
 * IE:
 *
 * "index in ['b' 'c' 'a' 'd']"
 *
 * Created by ctsims on 1/18/2017.
 */

public class IndexedSetMemberLookup implements PredicateProfile {
    public final String key;
    public final String[] valueSet;

    public IndexedSetMemberLookup(String key, Object valueSet) {
        this.key = key;
        this.valueSet = ((String)valueSet).split(" ");
    }

    public String getKey() {
        return key;
    }
}
