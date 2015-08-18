/**
 * 
 */
package org.commcare.core.models;

/**
 * @author ctsims
 *
 */
public interface EncryptedModel {
    public boolean isEncrypted(String data);
    public boolean isBlobEncrypted();
}
