package org.commcare.cases.query;

/**
 * A hint that a predicate has a structure that can potentially be optimized without
 * performing the full XPath engine evaluation.
 *
 * Profiles are used by (potentially platform specific) optimizations, but don't implement the
 * optimization itself
 *
 * For example, in the case of a predicate like:
 *
 * [@case_id = FOO]
 *
 * the expression can be evaluated without walking all child nodes, it can be matched with an index
 * lookup against a database.
 *
 * Created by ctsims on 1/18/2017.
 */

public interface PredicateProfile {

    /**
     * @return The index the predicate is operating over. This will be moved soon and isn't
     * a fundamental part of the structure.
     */
    String getKey();
}
