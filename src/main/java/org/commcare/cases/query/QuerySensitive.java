package org.commcare.cases.query;

/**
 * Query sensitive is an interface which lets an individual object be notified that it is about
 * to be referenced in a query context, to allow it to use any cached data available in the context
 * to prepare itself for use.
 *
 * Created by ctsims on 1/27/2017.
 */

public interface QuerySensitive {
    /**
     * Signals to this object that it is about to be used in the course of executing a query
     * whose context is provided here. That context may make it faster/easier for the object
     * to prepare itself for use.
     */
    void prepareForUseInCurrentContext(QueryContext queryContext);
}
