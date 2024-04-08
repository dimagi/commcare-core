package org.commcare.util;

import java.util.ServiceLoader;

/**
 * Utility class responsible for finding implementations of IEncryptionKeyProvider during runtime
 * and loading them in memory
 *
 * @author avazirna
 */

public class KeyStoreEncryptionKeyServiceProvider {
    private static KeyStoreEncryptionKeyServiceProvider serviceProvider;
    private ServiceLoader<IKeyStoreEncryptionKeyProvider> loader;

    private KeyStoreEncryptionKeyServiceProvider() {
        loader = ServiceLoader.load(IKeyStoreEncryptionKeyProvider.class);
    }

    public static KeyStoreEncryptionKeyServiceProvider getInstance() {
        if (serviceProvider == null) {
            serviceProvider = new KeyStoreEncryptionKeyServiceProvider();
        }
        return serviceProvider;
    }

    public IKeyStoreEncryptionKeyProvider serviceImpl() {
        IKeyStoreEncryptionKeyProvider service = null;
        if (loader.iterator().hasNext()) {
            service = loader.iterator().next();
        }

        return service;
    }
}

