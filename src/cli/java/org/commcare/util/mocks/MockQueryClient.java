package org.commcare.util.mocks;

import org.commcare.util.screen.QueryScreen;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import okhttp3.Request;

public class MockQueryClient implements QueryScreen.QueryClient {
    private InputStream mockResponse;

    public MockQueryClient(String mockResponse) {
        this.mockResponse = new ByteArrayInputStream(mockResponse.getBytes());;
    }

    public MockQueryClient(InputStream mockResponse) {
        this.mockResponse = mockResponse;
    }

    @Override
    public InputStream makeRequest(Request request) {
        return mockResponse;
    }
}
