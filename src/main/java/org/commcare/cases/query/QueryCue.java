package org.commcare.cases.query;

/**
 * A Query Cue carries forward information from a previous query handling which provides context
 * for a future set of queries to improve their efficiency when handling data.
 *
 * The Cue's have a lifecycle determined by when a significant query scope is being executed
 * and are expected to clean up after themselves if they have resulted in the queries retaining
 * more data in memory than usual
 *
 * Created by ctsims on 1/25/2017.
 */
public interface QueryCue {
    void cleanupCue();

    void activate();
}
