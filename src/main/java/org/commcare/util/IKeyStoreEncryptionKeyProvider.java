package org.commcare.util;

import java.security.Key;

/**
 * Service interface for Encryption Key providers for KeyStores
 *
 * @author avazirna
 */

public interface IKeyStoreEncryptionKeyProvider {

    Key generateCryptographicKeyInKeyStore(String keyAlias,
                                           EncryptionHelper.CryptographicOperation cryptographicOperation)
            throws EncryptionKeyHelper.EncryptionKeyException;

    String getTransformationString();

    String getKeyStoreName();
}
