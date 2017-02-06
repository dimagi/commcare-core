package org.commcare.cases.query.queryset;

import org.javarosa.core.model.instance.TreeReference;

/**
 * Created by ctsims on 2/6/2017.
 */

public interface QuerySetTransform {
    QuerySetLookup getTransformedLookup(QuerySetLookup incoming, TreeReference relativeLookup);
}
