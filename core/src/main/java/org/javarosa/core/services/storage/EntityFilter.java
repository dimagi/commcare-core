package org.javarosa.core.services.storage;

import java.util.Hashtable;

public abstract class EntityFilter<E> {

    public static final int PREFILTER_EXCLUDE = -1;
    public static final int PREFILTER_INCLUDE = 1;
    public static final int PREFILTER_FILTER = 0;

    /**
     * filter based just on ID and metadata (metadata not supported yet!! will always be 'null', currently)
     *
     * @return if PREFILTER_INCLUDE, record will be included, matches() not called
     * if PREFILTER_EXCLUDE, record will be excluded, matches() not called
     * if PREFILTER_FILTER, matches() will be called and record will be included or excluded based on return value
     */
    public int preFilter(int id, Hashtable<String, Object> metaData) {
        return PREFILTER_FILTER;
    }

    public abstract boolean matches(E e);
}
