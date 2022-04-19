package org.commcare.core.interfaces;

import org.javarosa.core.model.instance.VirtualDataInstance;

import java.sql.SQLException;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Read and write operations for entity selections made on a mult-select Entity Screen
 */
public interface VirtualDataInstanceCache {

    UUID write(VirtualDataInstance dataInstance);

    VirtualDataInstance read(UUID key);

    boolean contains(UUID key);
}
