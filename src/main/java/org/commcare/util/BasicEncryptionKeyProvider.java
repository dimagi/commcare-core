package org.commcare.util;

import static org.commcare.util.EncryptionHelper.CC_KEY_ALGORITHM_AES;
import static org.commcare.util.EncryptionHelper.CC_KEY_ALGORITHM_RSA;

public class BasicEncryptionKeyProvider implements IEncryptionKeyProvider {

    @Override
    public EncryptionKeyAndTransformation retrieveKeyFromKeyStore(String keyAlias,
            EncryptionHelper.CryptographicOperation operation) {
        return null;
    }

    @Override
    public void generateCryptographicKeyInKeyStore(String keyAlias) {
        // nothing, this version doesn't support a KeyStore
    }

    @Override
    public boolean isKeyStoreAvailable() {
        return false;
    }

    @Override
    public String getTransformationString(String algorithm) {
        String transformation = null;
        if (algorithm.equals(getAESKeyAlgorithmRepresentation())) {
            transformation = "AES/GCM/NoPadding";
        } else if (algorithm.equals(getRSAKeyAlgorithmRepresentation())) {
            transformation = "RSA/ECB/PKCS1Padding";
        }
        // This will cause an error
        return transformation;
    }

    @Override
    public String getAESKeyAlgorithmRepresentation() {
        return CC_KEY_ALGORITHM_AES;
    }

    @Override
    public String getRSAKeyAlgorithmRepresentation() {
        return CC_KEY_ALGORITHM_RSA;
    }

}
