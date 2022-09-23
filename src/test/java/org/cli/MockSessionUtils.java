package org.cli;

import com.google.common.collect.Multimap;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.PostRequest;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.screen.SessionUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;

/**
 * Mock version of SessionUtils that does not do requests
 */
public class MockSessionUtils extends SessionUtils {

    private InputStream mockQueryResponse = null;

    public MockSessionUtils() {}

    public MockSessionUtils(String mockResponse) {
        this.mockQueryResponse = new ByteArrayInputStream(mockResponse.getBytes());;
    }

    public MockSessionUtils(InputStream response) {
        this.mockQueryResponse = response;
    }

    @Override
    public void restoreUserToSandbox(UserSandbox sandbox, SessionWrapper session, CommCarePlatform platform,
            String username, String password, PrintStream printStream) {
    }

    @Override
    public int doPostRequest(PostRequest syncPost, SessionWrapper session, String username,
            String password, PrintStream printStream) throws IOException {
        return 201;
    }

    @Override
    public InputStream makeQueryRequest(URL url, Multimap<String, String> requestData, String username,
            String password) {
        return this.mockQueryResponse;
    }
}
