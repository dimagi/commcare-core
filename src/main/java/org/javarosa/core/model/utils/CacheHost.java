package org.javarosa.core.model.utils;

import org.javarosa.core.model.instance.TreeReference;

/**
 * @author ctsims
 */
public interface CacheHost {
    /**
     * Inside of this instance, identify the cache index that is associated with
     * the provided reference if possible. A null response indicates that a cache
     * entity ref is not available.
     *
     * A cache index must always represent the same object, although it is not
     * necessarily the case that two distinct indices won't represent the same
     * object.
     *
     * This behavior can be platform dependent, it is preferred that cache entities
     * are constant time entities that require no side computation, and is unlikely
     * that optimizations will work if they are not.
     */
    String getCacheIndex(TreeReference ref);

    /**
     * Evaluates whether the provided tree reference pattern can
     * be cached successfully. This should be requested _before_
     * retrieving the cache index for the ref pattern, as the cache
     * index ref is designed to be highly performant, so it is not
     * evaluated each time.
     *
     * This does _not_ guarantee that getCacheIndex() for this ref
     * or for other refs following this pattern will return a non-null
     * cache index, but merely that if one is returned that it will
     * be meaningful
     */
    boolean isReferencePatternCachable(TreeReference ref);

    /**
     * Get the set of parameters expected to yield the best results for
     * priming the cache based on the most recent query sets. Expected
     * is a set of String Key/Value pairs which make sense to the current
     * cache, ideally these pairs cover a close set of reference caches to
     * the most recent batch query.
     *
     * TODO: Super unclear whether this is the best place to put this
     *
     * @return a two-deep array of keys and values that are the same length, IE:
     * String[] keys = returvalue[0];
     * String[] values = returnvalue[1];
     */
    String[][] getCachePrimeGuess();
}
