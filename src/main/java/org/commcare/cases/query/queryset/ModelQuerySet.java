package org.commcare.cases.query.queryset;

import java.util.Collection;
import java.util.Set;

/**
 *
 * A ModelQuerySet is a set of identified models for which a member can be evaluated for
 * inclusion.
 *
 * ModelQuerySets can be evaluated for a member's presence and can also be transformed into a
 * derivative query sets.
 *
 * A model query set is genreally paired with a set of query set lookup objects. The query set
 * stores the state of the query results, while the lookup objects store the structure of the
 * optimized lookup possible based on the query set.
 *
 * As an example, the result of a predicate query over the Case database may result in hundreds of
 * matching cases. A later predicate operation may look for cases which are in that set, or may
 * apply a transform which generates a second query set based on, for instance, the set of "parent"
 * cases of the original set
 *
 * Keeping track of Model Query Sets lets the query planner identify operations which can be
 * simplified
 *
 * Created by ctsims on 1/25/2017.
 */
public interface ModelQuerySet {

    //
    Collection<Integer> getMatchingValues(Integer i);
    Set<Integer> getSetBody();
}
