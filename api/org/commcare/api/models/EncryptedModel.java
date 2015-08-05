/**
 * 
 */
package org.commcare.api.models;

/**
 * @author ctsims
 *
 */
public interface EncryptedModel {
    public boolean isEncrypted(String data);
    public boolean isBlobEncrypted();
}
