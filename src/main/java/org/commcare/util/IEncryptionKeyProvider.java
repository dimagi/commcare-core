package org.commcare.util;

/**
 * Service interface for Encryption Key providers for KeyStores
 *
 * @author avazirna
 */

public interface IEncryptionKeyProvider {

    void generateCryptographicKeyInKeyStore(String keyAlias);

    String getTransformationString();

    String getKeyStoreName();
}
