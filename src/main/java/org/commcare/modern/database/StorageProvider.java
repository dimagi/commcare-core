package org.commcare.modern.database;

import org.javarosa.core.services.storage.DummyExpressionCacheStorage;
import org.javarosa.core.services.storage.ExpressionCacheStorage;

/**
 * Created by amstone326 on 1/12/18.
 */

public class StorageProvider {

    private static StorageProvider instance = new StorageProvider();

    public static void setInstance(StorageProvider provider) {
        instance = provider;
    }

    public static StorageProvider instance() {
        return instance;
    }

    // -------------------------- //

    protected StorageProvider() {
    }

    public ExpressionCacheStorage getExpressionCacheStorage() {
        return new DummyExpressionCacheStorage();
    }
}
