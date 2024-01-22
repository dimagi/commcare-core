package org.commcare.util;

import javax.crypto.spec.SecretKeySpec;

public class EncryptionKeyHelper {

    // these key algorithm constants are to be used only outside of any Keystore scope
    public static final String CC_KEY_ALGORITHM_AES = "AES";
    public static final String CC_KEY_ALGORITHM_RSA = "RSA";

    public static final String CC_IN_MEMORY_ENCRYPTION_KEY_ALIAS = "cc-in-memory-encryption-key-alias";

    private static IEncryptionKeyProvider encryptionKeyProvider = EncryptionKeyServiceProvider.getInstance().serviceImpl();

    /**
     * Converts a Base64 encoded key into a SecretKey, PrivateKey or PublicKey, depending on the
     * algorithm and cryptographic operation
     *
     * @param algorithm              the algorithm to be used to encrypt/decrypt
     * @param base64encodedKey       key in String format
     * @param cryptographicOperation Cryptographic operation where the key is to be used, relevant
     * to the RSA algorithm
     * @return Secret key, Public key or Private Key to be used
     */
    private static EncryptionKeyAndTransformation getKey(String base64encodedKey)
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
                new SecretKeySpec(keyBytes, encryptionKeyProvider.getAESKeyAlgorithmRepresentation()),
                encryptionKeyProvider.getTransformationString(encryptionKeyProvider.getAESKeyAlgorithmRepresentation()));
    }
}
