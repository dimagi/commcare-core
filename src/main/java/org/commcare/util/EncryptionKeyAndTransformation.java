package org.commcare.util;

import java.security.Key;

/**
 * Utility class for holding an encryption key and transformation string pair
 *
 * @author dviggiano
 */
public class EncryptionKeyAndTransformation {
    private Key key;
    private String transformation;

    public EncryptionKeyAndTransformation(Key key, String transformation) {
        this.key = key;
        this.transformation = transformation;
    }

    public Key getKey() {
        return key;
    }

    public String getTransformation() {
        return transformation;
    }
}