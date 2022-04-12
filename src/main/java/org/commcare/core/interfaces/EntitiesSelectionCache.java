package org.commcare.core.interfaces;

import java.sql.SQLException;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Read and write operations for entity selections made on a mult-select Entity Screen
 */
public interface EntitiesSelectionCache {

    UUID write(String[] values);

    @Nullable
    String[] read(UUID key);

    boolean contains(UUID key);
}
