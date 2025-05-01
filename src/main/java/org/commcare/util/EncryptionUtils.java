package org.commcare.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtils {

    public static String encrypt(byte[] message, Key key, String transform,
                                        boolean includeMessageLength) throws EncryptionException {
        final int MIN_IV_LENGTH_BYTE = 1;
        final int MAX_IV_LENGTH_BYTE = 255;

        try {
            Cipher cipher = Cipher.getInstance(transform);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedMessage = cipher.doFinal(message);
            byte[] iv = cipher.getIV();
            if (iv.length < MIN_IV_LENGTH_BYTE || iv.length > MAX_IV_LENGTH_BYTE) {
                throw new EncryptionException("Initialization vector should be between " +
                        MIN_IV_LENGTH_BYTE + " and " + MAX_IV_LENGTH_BYTE +
                        " bytes long, but it is " + iv.length + " bytes");
            }


            int extraBytes = includeMessageLength ? 2 : 0;

            // The conversion of iv.length to byte takes the low 8 bits. To
            // convert back, cast to int and mask with 0xFF.
            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + iv.length + extraBytes + encryptedMessage.length)
                    .put((byte) iv.length)
                    .put(iv);

            if(includeMessageLength) {
                byteBuffer.put((byte)(encryptedMessage.length / 256));
                byteBuffer.put((byte)(encryptedMessage.length % 256));
            }

            byteBuffer.put(encryptedMessage);

            return Base64.encode(byteBuffer.array());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 BadPaddingException | IllegalBlockSizeException e) {
            throw new EncryptionException("Error during encryption", e);
        }
    }

    /**
     * Encrypts a message using the AES encryption and produces a base64 encoded payload containing the ciphertext, and a random IV which was used to encrypt the input.
     *
     * @param message a UTF-8 encoded message to be encrypted 
     * @param key     A base64 encoded 256 bit symmetric key
     * @return A base64 encoded payload containing the IV and AES encrypted ciphertext, which can be decoded by this utility's decrypt method and the same symmetric key
     */
    public static String encrypt(String message, String key) throws EncryptionException {
        final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
        SecretKey secret = getSecretKeySpec(key);

        return encrypt(message.getBytes(Charset.forName("UTF-8")),
                secret, ENCRYPT_ALGO, false);
    }


    public static SecretKey getSecretKeySpec(String key) throws EncryptionException {
        final int KEY_LENGTH_BIT = 256;
        byte[] keyBytes;
        try {
            keyBytes = Base64.decode(key);
        } catch (Base64DecoderException e) {
            throw new EncryptionException("Encryption key base 64 encoding is invalid", e);
        }
        if (8 * keyBytes.length != KEY_LENGTH_BIT) {
            throw new EncryptionException("Key should be " + KEY_LENGTH_BIT +
                    " bits long, not " + 8 * keyBytes.length);
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static byte[] decrypt(byte[] bytes, Key key, String keyAndTransform,
                                          boolean includeMessageLength)
            throws EncryptionException {
        final int TAG_LENGTH_BIT = 128;
        int readIndex = 0;
        int ivLength = bytes[readIndex] & 0xFF;
        readIndex++;
        if (ivLength < 0) {
            //Note: Early chance to catch decryption error
            throw new EncryptionException("Negative IV length");
        }
        byte[] iv = null;
        if (ivLength > 0) {
            iv = new byte[ivLength];
            System.arraycopy(bytes, readIndex, iv, 0, ivLength);
            readIndex += ivLength;
        }

        int encryptedLength;
        if(includeMessageLength) {
            encryptedLength= (bytes[readIndex] & 0xFF) << 8;
            readIndex++;
            encryptedLength += (bytes[readIndex] & 0xFF);
        } else {
            encryptedLength = bytes.length - readIndex;
        }

        byte[] encrypted = new byte[encryptedLength];
        readIndex++;
        System.arraycopy(bytes, readIndex, encrypted, 0, encryptedLength);

        try {
            Cipher cipher = Cipher.getInstance(keyAndTransform);

            if (includeMessageLength) {
                cipher.init(Cipher.DECRYPT_MODE, key, iv != null ? new IvParameterSpec(iv) : null);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            }

            return cipher.doFinal(encrypted);
        } catch(NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException |
                InvalidAlgorithmParameterException | BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypts a base64 payload containing an IV and AES encrypted ciphertext using the provided key
     *
     * @param message a message to be decrypted
     * @param key     key that should be used for decryption
     * @return Decrypted message for the given AES encrypted message
     */
    public static String decrypt(String message, String key) throws EncryptionException {
        final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
        SecretKey secret = getSecretKeySpec(key);

        try {
            byte[] messageBytes = Base64.decode(message);
            byte[] plainText = decrypt(messageBytes, secret, ENCRYPT_ALGO, false);

            return new String(plainText, Charset.forName("UTF-8"));
        } catch(Base64DecoderException e) {
            throw new EncryptionException("Decrypting message failed", e);
        }
    }

    public static String getMd5HashAsString(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte[] hashInBytes = md.digest();
            return Base64.encode(hashInBytes);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
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
