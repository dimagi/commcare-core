package org.commcare.cases.query.queryset;

import org.commcare.cases.query.PredicateProfile;
import org.javarosa.core.model.instance.TreeReference;

/**
 * A profile of an expression which can be matched to a query set lookup, which can be loaded in
 * bulk.
 *
 * Created by ctsims on 1/31/2017.
 */

public class ModelQueryLookup implements PredicateProfile {
    private String key;
    private TreeReference rootLookupRef;
    private QuerySetLookup setLookup;

    public ModelQueryLookup(String key, QuerySetLookup set, TreeReference rootLookupRef) {
        this.key = key;
        this.setLookup = set;
        this.rootLookupRef = rootLookupRef;
    }

    @Override
    public String getKey() {
        return key;
    }

    public QuerySetLookup getSetLookup() {
        return setLookup;
    }

    public TreeReference getRootLookupRef() {
        return rootLookupRef;
    }
}
