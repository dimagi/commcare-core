package org.commcare.cases.query;

/**
 * Created by ctsims on 1/27/2017.
 */

public interface QuerySensitive {
    void notifyOfCurrentQueryContext(QueryContext queryContext);
}
