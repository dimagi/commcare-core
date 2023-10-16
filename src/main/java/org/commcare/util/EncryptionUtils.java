package org.commcare.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.commcare.util.CommCarePlatform.getPlatformKeyStoreName;

public class EncryptionUtils {

    public static final String USER_CREDENTIALS_KEY_ALIAS = "user-credentials-key-alias";

    public static final String RSA_ALGORITHM_KEY = "RSA";
    public static final String AES_ALGORITHM_KEY = "AES";


    private static KeyStore platformKeyStore;

    private enum CryptographicOperation {Encryption, Decryption}

    public static KeyStore getPlatformKeyStore() {
        if (platformKeyStore == null) {
            try {
                platformKeyStore = KeyStore.getInstance(getPlatformKeyStoreName());
                platformKeyStore.load(null);
            } catch (KeyStoreException | IOException | NoSuchAlgorithmException |
                     CertificateException e) {
                throw new RuntimeException(e);
            }
        }
        return platformKeyStore;
    }

    public static String encryptUsingKeyFromKeyStore(String message, String alias) throws EncryptionException {
        Key key;
        try {
            key = retrieveKeyFromKeyStore(alias, CryptographicOperation.Encryption);
        } catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return encrypt(key.getAlgorithm(), message, key);
    }

    public static String encryptUsingBase64EncodedKey(String algorithm, String message, String key) throws EncryptionException {
        Key secret;
        try {
            secret = getKey(algorithm, key, CryptographicOperation.Encryption);
        } catch (InvalidKeySpecException e) {
            throw new EncryptionException("Invalid Key specifications", e);
        }
        return encrypt(algorithm, message, secret);
    }

    /**
     * Encrypts a message using the AES or RAS algorithms and produces a base64 encoded payload containing the ciphertext, and a random IV which was used to encrypt the input.
     *
     * @param algorithm to be used to encrypt the data
     * @param message   a UTF-8 encoded message to be encrypted
     * @param key       a SecretKey or PublicKey, depdending on the algorithm to be used
     * @return A base64 encoded payload containing the IV and AES encrypted ciphertext, which can be decoded by this utility's decrypt method and the same symmetric key
     */
    public static String encrypt(String algorithm, String message, Key key) throws EncryptionException {
        final int MIN_IV_LENGTH_BYTE = 1;
        final int MAX_IV_LENGTH_BYTE = 255;

        try {
            Cipher cipher = Cipher.getInstance(getCryptographicTransformation(algorithm));
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedMessage = cipher.doFinal(message.getBytes(Charset.forName("UTF-8")));
            byte[] iv = cipher.getIV();
            int ivSize = (iv == null ? 0 : iv.length);
            if (ivSize == 0) {
                iv = new byte[0];
            } else if (ivSize < MIN_IV_LENGTH_BYTE || ivSize > MAX_IV_LENGTH_BYTE) {
                throw new EncryptionException("Initialization vector should be between " +
                        MIN_IV_LENGTH_BYTE + " and " + MAX_IV_LENGTH_BYTE +
                        " bytes long, but it is " + ivSize + " bytes");
            }
            // The conversion of iv.length to byte takes the low 8 bits. To
            // convert back, cast to int and mask with 0xFF.
            byte[] ivPlusMessage = ByteBuffer.allocate(1 + ivSize + encryptedMessage.length)
                    .put((byte)ivSize)
                    .put(iv)
                    .put(encryptedMessage)
                    .array();
            return Base64.encode(ivPlusMessage);
        } catch (Exception ex) {
            throw new EncryptionException("Unknown error during encryption", ex);
        }
    }

    /**
     * Converts a base64 encoded key into a SecretKey, PrivateKey or PublicKey, depending on the
     * Algorithm and Cryptographic operation
     *
     * @param algorithm              to be used to encrypt/decrypt
     * @param base64encodedKey       key in String format
     * @param cryptographicOperation relevant to the RSA algorithm
     * @return Decrypted message for the given AES encrypted message
     */
    private static Key getKey(String algorithm, String base64encodedKey, CryptographicOperation cryptographicOperation) throws EncryptionException, InvalidKeySpecException {
        byte[] keyBytes;
        try {
            keyBytes = Base64.decode(base64encodedKey);
        } catch (Base64DecoderException e) {
            throw new EncryptionException("Encryption key base 64 encoding is invalid", e);
        }

        if (algorithm.equals(AES_ALGORITHM_KEY)) {
            final int KEY_LENGTH_BIT = 256;

            if (8 * keyBytes.length != KEY_LENGTH_BIT) {
                throw new EncryptionException("Key should be " + KEY_LENGTH_BIT +
                        " bits long, not " + 8 * keyBytes.length);
            }
            return new SecretKeySpec(keyBytes, AES_ALGORITHM_KEY);
        } else if (algorithm.equals(RSA_ALGORITHM_KEY)) {
            // This is not very relevant at the moment as the RSA algorithm is only used to encrypt
            // user credentials on devices runnning Android 5.0 - 5.1.1 for the KeyStore
            KeyFactory keyFactory = null;
            try {
                keyFactory = KeyFactory.getInstance(RSA_ALGORITHM_KEY);
            } catch (NoSuchAlgorithmException e) {
                throw new EncryptionException("There is no Provider for the selected algorithm", e);
            }

            if (cryptographicOperation == CryptographicOperation.Encryption) {
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
                return keyFactory.generatePublic(keySpec);
            } else {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
                return keyFactory.generatePrivate(keySpec);
            }
        }
        // This should cause an error
        return null;
    }

    private static String getCryptographicTransformation(String algorithm) {
        if (algorithm.equals(AES_ALGORITHM_KEY)) {
            return "AES/GCM/NoPadding";
        } else if (algorithm.equals(RSA_ALGORITHM_KEY)) {
            return "RSA/ECB/PKCS1Padding";
        } else {
            // This will cause an error
            return null;
        }
    }

    private static Key retrieveKeyFromKeyStore(String keyAlias, CryptographicOperation operation) throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException {
        if (getPlatformKeyStore().containsAlias(keyAlias)) {
            KeyStore.Entry keyEntry = getPlatformKeyStore().getEntry(keyAlias, null);
            if (keyEntry instanceof KeyStore.PrivateKeyEntry) {
                if (operation == CryptographicOperation.Encryption) {
                    return ((KeyStore.PrivateKeyEntry)keyEntry).getCertificate().getPublicKey();
                } else {
                    return ((KeyStore.PrivateKeyEntry)keyEntry).getPrivateKey();
                }
            } else {
                return ((KeyStore.SecretKeyEntry)keyEntry).getSecretKey();
            }
        } else {
            throw new KeyStoreException("Key not found in KeyStore");
        }
    }

    public static String decryptUsingKeyFromKeyStore(String message, String alias) throws EncryptionException {
        Key key;
        try {
            key = retrieveKeyFromKeyStore(alias, CryptographicOperation.Decryption);
        } catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return decrypt(key.getAlgorithm(), message, key);
    }

    public static String decryptUsingBase64EncodedKey(String algorithm, String message, String key) throws EncryptionException {
        Key secret = null;
        try {
            secret = getKey(algorithm, key, CryptographicOperation.Decryption);
        } catch (InvalidKeySpecException e) {
            throw new EncryptionException("Invalid Key specifications", e);
        }
        return decrypt(algorithm, message, secret);
    }

    /**
     * Decrypts a base64 payload containing an IV and AES encrypted ciphertext using the provided key
     *
     * @param message a message to be decrypted
     * @param key     key that should be used for decryption
     * @return Decrypted message for the given AES encrypted message
     */
    private static String decrypt(String algorithm, String message, Key key) throws EncryptionException {
        final int TAG_LENGTH_BIT = 128;

        try {
            byte[] messageBytes = Base64.decode(message);
            ByteBuffer bb = ByteBuffer.wrap(messageBytes);
            int iv_length_byte = bb.get() & 0xFF;
            byte[] iv = new byte[iv_length_byte];
            bb.get(iv);

            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);

            Cipher cipher = Cipher.getInstance(getCryptographicTransformation(algorithm));
            if (algorithm.equals(AES_ALGORITHM_KEY)) {
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key);
            }
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, Charset.forName("UTF-8"));
        } catch (NoSuchAlgorithmException | BadPaddingException | NoSuchPaddingException |
                 IllegalBlockSizeException | InvalidKeyException | Base64DecoderException |
                 InvalidAlgorithmParameterException e) {
            throw new EncryptionException("Error encountered while decrypting the message", e);
        }
    }

    public static boolean isPlatformKeyStoreAvailable() {
        return Security.getProvider(getPlatformKeyStoreName()) != null;
    }

    public static class EncryptionException extends Exception {

        public EncryptionException(String message) {
            super(message);
        }

        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
