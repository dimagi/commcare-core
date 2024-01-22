package org.commcare.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

import static org.commcare.util.EncryptionKeyHelper.encryptionKeyProvider;

public class EncryptionHelper {

    public enum CryptographicOperation {Encryption, Decryption}

    /**
     * Encrypts a message using a key stored in the platform KeyStore. The key is retrieved using
     * its alias which is established during key generation.
     *
     * @param message  a UTF-8 encoded message to be encrypted
     * @param keyAlias key alias of the Key stored in the KeyStore, depending on the algorithm,
     *                 it can be a SecretKey (for AES) or PublicKey (for RSA) to be used to
     *                 encrypt the message
     * @return A base64 encoded payload containing the IV and AES or RSA encrypted ciphertext,
     *         which can be decoded by this utility's decrypt method and the same key
     * @throws KeyStoreException           if the keystore has not been initialized
     * @throws NoSuchAlgorithmException    if the appropriate data integrity algorithm could not be
     *                                     found
     * @throws UnrecoverableEntryException if an entry in the keystore cannot be retrieved
     */
    public String encryptWithKeyStore(String message, String keyAlias)
            throws UnrecoverableEntryException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        EncryptionKeyAndTransformation keyAndTransformation = EncryptionKeyProvider.retrieveKeyFromKeyStore(keyAlias, CryptographicOperation.Encryption);


        try {
            return encrypt(message, keyAndTransformation);
        } catch (EncryptionException e) {
            throw new RuntimeException("Error encountered while encrypting the data: ", e);
        }
    }

    public String encryptWithBase64EncodedKey(String message, String key)
            throws EncryptionException {
        EncryptionKeyAndTransformation keyAndTransformation;
        try {
            keyAndTransformation = EncryptionKeyHelper.getKey(key);
        } catch (InvalidKeySpecException e) {
            throw new EncryptionException("Invalid Key specifications", e);
        }
        return encrypt(message, keyAndTransformation);
    }

    /**
     * Encrypts a message using the AES or RAS algorithms and produces a base64 encoded payload
     * containing the ciphertext, and, when applicable, a random IV which was used to encrypt
     * the input.
     *
     * @param message          a UTF-8 encoded message to be encrypted
     * @param keyAndTransform  depending on the algorithm, a SecretKey or PublicKey, and
     *                         cryptographic transformation to be used to encrypt the message
     * @return  A base64 encoded payload containing the IV and AES or RSA encrypted ciphertext,
     *          which can be decoded by this utility's decrypt method and the same key
     */
    private String encrypt(String message, EncryptionKeyAndTransformation keyAndTransform)
            throws EncryptionException {
        final int MIN_IV_LENGTH_BYTE = 1;
        final int MAX_IV_LENGTH_BYTE = 255;

        try {
            Cipher cipher = Cipher.getInstance(keyAndTransform.getTransformation());
            cipher.init(Cipher.ENCRYPT_MODE, keyAndTransform.getKey());
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
     * Decrypts a base64 payload containing an IV and AES or RSA encrypted ciphertext using a key
     * stored in the platform KeyStore. The key is retrieved using its alias which is established
     * during key generation.
     *
     * @param message  a UTF-8 encoded message to be decrypted
     * @param keyAlias key alias of the Key stored in the KeyStore, depending on the algorithm,
     *                 it can be a SecretKey (for AES) or PublicKey (for RSA) to be used to
     *                 decrypt the message
     * @return Decrypted message of the provided ciphertext,
     * @throws KeyStoreException           if the keystore has not been initialized
     * @throws NoSuchAlgorithmException    if the appropriate data integrity algorithm could not be
     *                                     found
     * @throws UnrecoverableEntryException if an entry in the keystore cannot be retrieved
     */
    public String decryptWithKeyStore(String message, String keyAlias)
            throws UnrecoverableEntryException, KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException {
        EncryptionKeyAndTransformation keyAndTransformation =
                EncryptionKeyHelper.getKey(keyAlias, CryptographicOperation.Decryption);
        try {
            return decrypt(message, keyAndTransformation);
        } catch (EncryptionException e) {
            throw new RuntimeException("Error encountered while decrypting the data ", e);
        }
    }

    public String decryptWithBase64EncodedKey(String message, String key)
            throws EncryptionException {
        EncryptionKeyAndTransformation keyAndTransformation;
        try {
            keyAndTransformation = EncryptionKeyHelper.getKey(key);
        } catch (InvalidKeySpecException e) {
            throw new EncryptionException("Invalid Key specifications", e);
        }
        return decrypt(message, keyAndTransformation);
    }

    /**
     * Decrypts a base64 payload containing an IV and AES or RSA encrypted ciphertext using the
     * provided key
     *
     * @param message         a message to be decrypted
     * @param keyAndTransform depending on the algorithm, a Secret key or Private key and its
     *                        respective cryptographic transformation to be used for decryption
     * @return Decrypted message for the given encrypted message
     */
    private String decrypt(String message, EncryptionKeyAndTransformation keyAndTransform)
            throws EncryptionException {
        final int TAG_LENGTH_BIT = 128;

        try {
            byte[] messageBytes = Base64.decode(message);
            ByteBuffer bb = ByteBuffer.wrap(messageBytes);
            int iv_length_byte = bb.get() & 0xFF;
            byte[] iv = new byte[iv_length_byte];
            bb.get(iv);

            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);

            Cipher cipher = Cipher.getInstance(keyAndTransform.getTransformation());
            if (keyAndTransform.getKey().getAlgorithm().equals(EncryptionKeyProvider.getAESKeyAlgorithmRepresentation())) {
                cipher.init(Cipher.DECRYPT_MODE, keyAndTransform.getKey(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            } else {
                cipher.init(Cipher.DECRYPT_MODE, keyAndTransform.getKey());
            }
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, Charset.forName("UTF-8"));
        } catch (NoSuchAlgorithmException | BadPaddingException | NoSuchPaddingException |
                 IllegalBlockSizeException | InvalidKeyException | Base64DecoderException |
                 InvalidAlgorithmParameterException e) {
            throw new EncryptionException("Error encountered while decrypting the message", e);
        }
    }

    public class EncryptionException extends Exception {

        public EncryptionException(String message) {
            super(message);
        }

        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
