package org.commcare.core.interfaces;

import java.io.IOException;
import java.io.InputStream;

public interface ResponseStreamAccessor {
    InputStream getResponseStream() throws IOException;

    InputStream getErrorResponseStream();
    String getApiVersion();
}
