package org.commcare.cases.query.queryset;

import java.util.Collection;
import java.util.Set;

/**
 * A ModelQuerySet is a very basic data type which stores the result of a query set lookup. It
 * maintains one-to-many mapping of model id's (IE: Cases in the case db) which are the result
 * of a potentially complex query, and allow individual values to be returned, as well as providing
 * a way to get all matching values to perform bulk operations.
 *
 * Created by ctsims on 1/25/2017.
 */
public interface ModelQuerySet {
    Collection<Integer> getMatchingValues(Integer i);
    Set<Integer> getSetBody();
}
