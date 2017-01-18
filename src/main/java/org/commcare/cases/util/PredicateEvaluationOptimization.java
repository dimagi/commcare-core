package org.commcare.cases.util;

/**
 * A template for a way to optimize the evaluation of specific patterns of predicates against
 * indexes or databases.
 *
 * For example, an optimization might provide the structure to notice that
 *
 * @case_id = FOO
 *
 * can be optimized by looking up the case by its ID in a database rather than by performing a
 * full walk.
 *
 * Created by ctsims on 1/18/2017.
 */

public interface PredicateEvaluationOptimization {
    public String getKey();
}
