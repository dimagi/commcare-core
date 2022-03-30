package org.commcare.core.interfaces;

import java.sql.SQLException;

/**
 * Read and write operations for entity selections made on a mult-select Entity Screen
 */
public interface EntitiesSelectionCache {

    void cache(String key, String[] values) throws SQLException;

    String[] read(String key);
}
