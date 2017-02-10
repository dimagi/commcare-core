package org.commcare.cases.query.queryset;

import org.javarosa.core.model.instance.TreeReference;

import java.util.Collection;
import java.util.Set;

/**
 * Created by ctsims on 2/6/2017.
 */

public class CurrentModelQuerySet implements ModelQuerySet {
    public static final String CURRENT_QUERY_SET_ID = "current";

    private Collection<TreeReference> currentQuerySet;

    public CurrentModelQuerySet(Collection<TreeReference> currentQuerySet) {
        this.currentQuerySet = currentQuerySet;
    }

    public Collection<TreeReference> getCurrentQuerySet() {
        return currentQuerySet;
    }

    //the below is a hack and is bad.

    //This shouldn't actually be a model query set as outlined currently, but it's gonna take
    //a bit to pull it into its own caching model
    @Override
    public Collection<Integer> getMatchingValues(Integer i) {
        return null;
    }

    @Override
    public Set<Integer> getSetBody() {
        return null;
    }
}
