package org.commcare.util;

import java.util.NoSuchElementException;
import java.util.ServiceLoader;

/**
 * Utility class responsible for finding implementations of IEncryptionKeyProvider during runtime
 * and loading them in memory
 *
 * @author avazirna
 */

public class EncryptionKeyServiceProvider {
    private static EncryptionKeyServiceProvider serviceProvider;
    private ServiceLoader<IEncryptionKeyProvider> loader;

    private EncryptionKeyServiceProvider() {
        loader = ServiceLoader.load(IEncryptionKeyProvider.class);
    }

    public static EncryptionKeyServiceProvider getInstance() {
        if (serviceProvider == null) {
            serviceProvider = new EncryptionKeyServiceProvider();
        }
        return serviceProvider;
    }

    public IEncryptionKeyProvider serviceImpl() {
        IEncryptionKeyProvider service = null;
        if (loader.iterator().hasNext()) {
            service = loader.iterator().next();
        }

        if (service != null) {
            return service;
        } else {
            throw new NoSuchElementException(
                    "No implementation for IEncryptionKeyProvider");
        }
    }
}

