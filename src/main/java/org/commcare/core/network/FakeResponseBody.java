package org.commcare.core.network;

import java.io.InputStream;

import javax.annotation.Nullable;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;

public class FakeResponseBody extends ResponseBody {
    private final InputStream inputStream;

    public FakeResponseBody(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return null;
    }

    @Override
    public long contentLength() {
        return -1;
    }

    @Override
    public BufferedSource source() {
        return Okio.buffer(Okio.source(inputStream));
    }
}
