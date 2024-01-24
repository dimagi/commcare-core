package org.commcare.core.network.bitcache;

import org.commcare.util.EncryptionKeyHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author ctsims
 */
public interface BitCache {
    void initializeCache() throws IOException, EncryptionKeyHelper.EncryptionKeyException;

    OutputStream getCacheStream() throws IOException;

    InputStream retrieveCache() throws IOException;

    void release();
}
