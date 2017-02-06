package org.commcare.cases.query.queryset;

import org.commcare.cases.query.QueryContext;

import java.util.Collection;
import java.util.Set;

/**
 *
 * A Model Query Set set is a set of identified models for which a member can be evaluated for
 * inclusion.
 *
 * Model Query Sets can be evaluated for a member's presence and can also be transformed into a
 * derivative query sets.
 *
 * As an example, the result of a predicate query over the Case database may result in hundreds of
 * matching cases. A later predicate operation may look for cases which are in that set, or may
 * apply a transform which generates a second query set based on, for instance, the set of "parent"
 * cases of the original set
 *
 * Created by ctsims on 1/25/2017.
 */
public interface ModelQuerySet {
    Collection<Integer> getMatchingValues(Integer i);

    Set<Integer> getSetBody();
}
