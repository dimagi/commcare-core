package org.commcare.core.interfaces;

import java.sql.SQLException;

import javax.annotation.Nullable;

/**
 * Read and write operations for entity selections made on a mult-select Entity Screen
 */
public interface EntitiesSelectionCache {

    String cache(String[] values) throws SQLException;

    @Nullable
    String[] read(String key) throws SQLException;

    boolean contains(String key) throws SQLException;
}
