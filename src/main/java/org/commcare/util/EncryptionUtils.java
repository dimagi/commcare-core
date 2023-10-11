package org.commcare.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtils {
    private static KeyStore androidKeyStore;

    public static final String USER_CREDENTIALS_KEY_ALIAS = "user-credentials-key-alias";

    public static final String ANDROID_KEYSTORE_PROVIDER_NAME = "AndroidKeyStore";

    private enum CryptographicOperation {Encryption, Decryption}

    public static KeyStore getAndroidKeyStore(){
        if (androidKeyStore == null) {
            try {
                androidKeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER_NAME);
                androidKeyStore.load(null);
            } catch (KeyStoreException | IOException | NoSuchAlgorithmException |
                     CertificateException e) {
                throw new RuntimeException(e);
            }
        }
        return androidKeyStore;
    }

    /**
     * Encrypts a message using the AES encryption and produces a base64 encoded payload containing the ciphertext, and a random IV which was used to encrypt the input.
     *
     * @param message a UTF-8 encoded message to be encrypted
     * @param keyOrAlias A base64 encoded 256 bit symmetric key OR a KeyStore key alias
     * @param usingKey indicate whether a Key or a Key alias was provided
     * @return A base64 encoded payload containing the IV and AES encrypted ciphertext, which can be decoded by this utility's decrypt method and the same symmetric key
     */
    public static String encrypt(String message, String keyOrAlias, boolean usingKey) throws EncryptionException {
        final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
        final int MIN_IV_LENGTH_BYTE = 1;
        final int MAX_IV_LENGTH_BYTE = 255;
        Key secret = getKey(keyOrAlias, usingKey, CryptographicOperation.Encryption);

        try {
            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            byte[] encryptedMessage = cipher.doFinal(message.getBytes(Charset.forName("UTF-8")));
            byte[] iv = cipher.getIV();
            if (iv.length < MIN_IV_LENGTH_BYTE || iv.length > MAX_IV_LENGTH_BYTE) {
                throw new EncryptionException("Initialization vector should be between " +
                        MIN_IV_LENGTH_BYTE + " and " + MAX_IV_LENGTH_BYTE +
                        " bytes long, but it is " + iv.length + " bytes");
            }
            // The conversion of iv.length to byte takes the low 8 bits. To
            // convert back, cast to int and mask with 0xFF.
            byte[] ivPlusMessage = ByteBuffer.allocate(1 + iv.length + encryptedMessage.length)
                    .put((byte)iv.length)
                    .put(iv)
                    .put(encryptedMessage)
                    .array();
            return Base64.encode(ivPlusMessage);
        } catch (Exception ex) {
            throw new EncryptionException("Unknown error during encryption", ex);
        }
    }

    private static Key getKey(String keyOrAlias, boolean usingKey, CryptographicOperation operation) throws EncryptionException {
        try {
            if (usingKey) {
                return createKeyFromBase64String(keyOrAlias);
            }
            else {
                return retrieveKeyFromKeyStore(keyOrAlias, operation);
            }
        } catch (EncryptionException e) {
            throw new EncryptionException(e.getMessage());
        } catch (UnrecoverableEntryException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static Key createKeyFromBase64String(String base64encodedKey) throws EncryptionException {
        final int KEY_LENGTH_BIT = 256;
        byte[] keyBytes;
        try {
            keyBytes = Base64.decode(base64encodedKey);
        } catch (Base64DecoderException e) {
            throw new EncryptionException("Encryption key base 64 encoding is invalid", e);
        }
        if (8 * keyBytes.length != KEY_LENGTH_BIT) {
            throw new EncryptionException("Key should be " + KEY_LENGTH_BIT +
                    " bits long, not " + 8 * keyBytes.length);
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static Key retrieveKeyFromKeyStore(String keyAlias, CryptographicOperation operation) throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException {
        if (getAndroidKeyStore().containsAlias(keyAlias)){
            KeyStore.Entry keyEntry = getAndroidKeyStore().getEntry(keyAlias, null);
            if (keyEntry instanceof KeyStore.PrivateKeyEntry){
                if (operation == CryptographicOperation.Encryption){
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

    /**
     * Decrypts a base64 payload containing an IV and AES encrypted ciphertext using the provided key
     *
     * @param message a message to be decrypted
     * @param keyOrAlias key or key alias that should be used for decryption
     * @return Decrypted message for the given AES encrypted message
     */
    public static String decrypt(String message, String keyOrAlias, boolean usingKey) throws EncryptionException {
        final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
        final int TAG_LENGTH_BIT = 128;
        Key secret = getKey(keyOrAlias, usingKey, CryptographicOperation.Decryption);

        try {
            byte[] messageBytes = Base64.decode(message);
            ByteBuffer bb = ByteBuffer.wrap(messageBytes);
            int iv_length_byte = bb.get() & 0xFF;
            byte[] iv = new byte[iv_length_byte];
            bb.get(iv);

            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);


            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, Charset.forName("UTF-8"));
        } catch (NoSuchAlgorithmException | BadPaddingException | NoSuchPaddingException |
                IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeyException | Base64DecoderException e) {
            throw new EncryptionException("Error encountered while decrypting the message", e);
        }
    }

    public static boolean isAndroidKeyStoreSupported(){
        return Security.getProvider("AndroidKeyStore") != null;
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
