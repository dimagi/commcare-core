package org.commcare.util;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.spec.SecretKeySpec;

public class EncryptionKeyHelper {

    // these key algorithm constants are to be used only outside of any Keystore scope
    public static final String CC_KEY_ALGORITHM_AES = "AES";
    public static final String CC_KEY_ALGORITHM_RSA = "RSA";
    public static final String CC_IN_MEMORY_ENCRYPTION_KEY_ALIAS = "cc-in-memory-encryption-key-alias";

    private static IEncryptionKeyProvider encryptionKeyProvider = EncryptionKeyServiceProvider.getInstance().serviceImpl();

    /**
     * Converts a Base64 encoded key into a SecretKey depending on the algorithm
     *
     * @param base64encodedKey       key in String format
     * @return Secret key to be used to encrypt/decrypt data
     */
    public static EncryptionKeyAndTransformation getKey(String base64encodedKey)
            throws EncryptionHelper.EncryptionException, InvalidKeySpecException {
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

    public static boolean isKeyStoreAvailable() {
        return Security.getProvider(encryptionKeyProvider.getKeyStoreName()) != null;
    }

    private static KeyStore keystoreSingleton = null;

    private static KeyStore getKeyStore() throws KeyStoreException, CertificateException,
            IOException, NoSuchAlgorithmException {
        if (keystoreSingleton == null) {
            keystoreSingleton = KeyStore.getInstance(encryptionKeyProvider.getKeyStoreName());
            keystoreSingleton.load(null);
        }
        return keystoreSingleton;
    }

    /**
     * Returns a key stored in the KeyStore, PrivateKey or PublicKey, depending on the
     * cryptographic operation
     *
     * @param keyAlias                key in String format
     * @param cryptographicOperation  Cryptographic operation where the key is to be used, relevant
     *                                to the RSA algorithm
     * @return Public key or Private Key to be used to encrypt/decrypt data
     */
    public static EncryptionKeyAndTransformation retrieveKeyFromKeyStore(String keyAlias,
                                                                         EncryptionHelper.CryptographicOperation cryptographicOperation)
            throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException,
            CertificateException, IOException {
        Key key;
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
            key = encryptionKeyProvider.generateCryptographicKeyInKeyStore(keyAlias, cryptographicOperation);
        }
        if (key != null) {
            return new EncryptionKeyAndTransformation(key, encryptionKeyProvider.getTransformationString());
        } else {
            return null;
        }
    }
}
