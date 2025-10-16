package org.commcare.core.interfaces;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

public interface ResponseStreamAccessor {
    InputStream getResponseStream() throws IOException;

    @Nullable
    InputStream getErrorResponseStream() throws IOException;
    String getApiVersion();
}
