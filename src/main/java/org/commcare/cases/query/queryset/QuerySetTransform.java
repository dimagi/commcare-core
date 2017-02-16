package org.commcare.cases.query.queryset;

import org.javarosa.core.model.instance.TreeReference;

/**
 * Provides a query set matcher the ability to transform a query set from one type of query
 * (or one model) to another.
 *
 * Potentially useful for both transforming within a model (IE: go from "current cases" to "cases
 * that are the parents of current cases") and transforming between models (IE: go from "current
 * case" to "ledger value") in ways that can bypass needing internal indirection.
 *
 * Created by ctsims on 2/6/2017.
 */

public interface QuerySetTransform {
    QuerySetLookup getTransformedLookup(QuerySetLookup incoming, TreeReference relativeLookup);
}
