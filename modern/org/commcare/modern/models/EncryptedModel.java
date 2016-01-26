package org.commcare.modern.models;

/**
 * @author ctsims
 */
public interface EncryptedModel {
    boolean isEncrypted(String data);

    boolean isBlobEncrypted();
}
