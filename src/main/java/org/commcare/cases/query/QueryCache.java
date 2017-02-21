package org.commcare.cases.query;

/**
 * A query cache stores data local to a query context which will be relevant for recomputation
 * within the lifecycle of the Query.
 *
 * QueryCache is a tag intance that is just used to tag classes which will be used with the
 * QueryCacheHost lifeycle.
 *
 * QueryCache's must have a public constructor with no arguments, and there will only ever be one
 * in use at a time.
 *
 * Created by ctsims on 1/26/2017.
 */

public interface QueryCache {
}
