package org.commcare.core.interfaces;

import org.commcare.util.EncryptionKeyHelper;

import java.io.IOException;
import java.io.InputStream;

public interface ResponseStreamAccessor {
    InputStream getResponseStream() throws IOException, EncryptionKeyHelper.EncryptionKeyException;
}
