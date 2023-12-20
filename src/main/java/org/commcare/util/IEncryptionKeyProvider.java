package org.commcare.util;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

/**
 * Service interface for Encryption Key providers for KeyStores
 *
 * @author avazirna
 */

public interface IEncryptionKeyProvider {

    EncryptionKeyAndTransformation retrieveKeyFromKeyStore(String keyAlias,
                                                           EncryptionHelper.CryptographicOperation operation)
            throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException, CertificateException, IOException;

    void generateCryptographicKeyInKeyStore(String keyAlias);

    boolean isKeyStoreAvailable();

    String getTransformationString(String algorithm);

    String getAESKeyAlgorithmRepresentation();

    String getRSAKeyAlgorithmRepresentation();
}
