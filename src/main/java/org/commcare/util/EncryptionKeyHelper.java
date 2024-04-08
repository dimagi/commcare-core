package org.commcare.util;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.spec.SecretKeySpec;

public class EncryptionKeyHelper {

    // these key algorithm constants are to be used only outside of any Keystore scope
    public static final String CC_KEY_ALGORITHM_AES = "AES";
    public static final String CC_KEY_ALGORITHM_RSA = "RSA";
    public static final String CC_IN_MEMORY_ENCRYPTION_KEY_ALIAS = "cc-in-memory-encryption-key-alias";

    private static final IKeyStoreEncryptionKeyProvider keyStoreEncryptionKeyProvider = KeyStoreEncryptionKeyServiceProvider.getInstance().serviceImpl();

    private static KeyStore keystoreSingleton = null;

    private static KeyStore getKeyStore() throws EncryptionKeyException {
        if (keystoreSingleton == null) {
            try {
                keystoreSingleton = KeyStore.getInstance(keyStoreEncryptionKeyProvider.getKeyStoreName());
                keystoreSingleton.load(null);
            } catch (KeyStoreException | CertificateException | IOException |
                     NoSuchAlgorithmException e) {
                throw new EncryptionKeyException("KeyStore failed to initialize", e);
            }
        }
        return keystoreSingleton;
    }

    /**
     * Converts a Base64 encoded key into a SecretKey depending on the algorithm
     *
     * @param base64encodedKey       key in String format
     * @return Secret key to be used to encrypt/decrypt data
     */
    public static EncryptionKeyAndTransformation retrieveKeyFromEncodedKey(String base64encodedKey)
            throws EncryptionKeyException {
        final int KEY_LENGTH_BIT = 256;
        byte[] keyBytes;
        try {
            keyBytes = Base64.decode(base64encodedKey);
        } catch (Base64DecoderException e) {
            throw new EncryptionKeyException("Encryption key base 64 encoding is invalid", e);
        }

        if (8 * keyBytes.length != KEY_LENGTH_BIT) {
            throw new EncryptionKeyException("Key should be " + KEY_LENGTH_BIT +
                    " bits long, not " + 8 * keyBytes.length);
        }
        return new EncryptionKeyAndTransformation(
                new SecretKeySpec(keyBytes, CC_KEY_ALGORITHM_AES),
                "AES/GCM/NoPadding");
    }

    private static boolean isKeyStoreAvailable() {
        return keyStoreEncryptionKeyProvider != null &&
                Security.getProvider(keyStoreEncryptionKeyProvider.getKeyStoreName()) != null;
    }

    /**
     * Returns an EncryptionKeyAndTransformation object that wraps a SecretKey, PrivateKey or
     * PublicKey, depending on the cryptographic operation and the cryptographic transformation.
     * This method generates a new key in case the alias doesn't exist.
     *
     * @param keyAlias                alias of the key stored in the KeyStore
     * @param cryptographicOperation  Cryptographic operation where the key is to be used, relevant
     *                                to the RSA algorithm
     * @return EncryptionKeyAndTransformation to be used to encrypt/decrypt data
     */
    public static EncryptionKeyAndTransformation retrieveKeyFromKeyStore(String keyAlias,
                                                                         EncryptionHelper.CryptographicOperation cryptographicOperation)
            throws EncryptionKeyException {
        if (!isKeyStoreAvailable()) {
            throw new EncryptionKeyException("No KeyStore facility available!");
        }
        Key key;
        try {
            if (getKeyStore().containsAlias(keyAlias)) {
                KeyStore.Entry keyEntry = getKeyStore().getEntry(keyAlias, null);
                if (keyEntry instanceof KeyStore.PrivateKeyEntry) {
                    if (cryptographicOperation == EncryptionHelper.CryptographicOperation.Encryption) {
                        key = ((KeyStore.PrivateKeyEntry)keyEntry).getCertificate().getPublicKey();
                    } else {
                        key = ((KeyStore.PrivateKeyEntry)keyEntry).getPrivateKey();
                    }
                } else {
                    key = ((KeyStore.SecretKeyEntry)keyEntry).getSecretKey();
                }
            } else {
                key = keyStoreEncryptionKeyProvider.generateCryptographicKeyInKeyStore(keyAlias, cryptographicOperation);
            }
        } catch (KeyStoreException| NoSuchAlgorithmException | UnrecoverableEntryException e) {
            throw new EncryptionKeyException("Error retrieving key from KeyStore", e);
        }
        if (key != null) {
            return new EncryptionKeyAndTransformation(key, keyStoreEncryptionKeyProvider.getTransformationString());
        } else {
            throw new EncryptionKeyException("Key couldn't be found in the keyStore");
        }
    }

    public static class EncryptionKeyException extends Exception {

        public EncryptionKeyException(String message) {
            super(message);
        }

        public EncryptionKeyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
