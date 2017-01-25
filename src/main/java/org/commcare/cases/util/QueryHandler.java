package org.commcare.cases.util;

import java.util.Vector;

/**
 * A tunable host for areas in storage which need potentially complex caching semantics
 * which rely on some structural knowledge. In many implementations will be quite simple
 *
 * Created by ctsims on 1/25/2017.
 */

public interface QueryHandler<T> {

    int getExpectedRuntime();


    T profileHandledQuerySet(Vector<PredicateProfile> profiles);

    Vector<Integer> loadProfileMatches(T querySet);

    void updateProfiles(T querySet, Vector<PredicateProfile> profiles);
}
