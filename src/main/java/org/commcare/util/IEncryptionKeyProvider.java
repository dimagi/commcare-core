package org.commcare.util;

import java.security.Key;

/**
 * Service interface for Encryption Key providers for KeyStores
 *
 * @author avazirna
 */

public interface IEncryptionKeyProvider {

    Key generateCryptographicKeyInKeyStore(String keyAlias,
                                           EncryptionHelper.CryptographicOperation cryptographicOperation);

    String getTransformationString();

    String getKeyStoreName();
}
