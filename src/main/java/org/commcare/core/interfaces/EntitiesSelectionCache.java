package org.commcare.core.interfaces;

import java.sql.SQLException;

public interface EntitiesSelectionCache {

    void cache(String key, String[] values) throws SQLException;

    String[] read(String key);
}
