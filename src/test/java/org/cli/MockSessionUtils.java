package org.cli;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.PostRequest;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.screen.SessionUtils;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Mock version of SessionUtils that does not do requests
 */
public class MockSessionUtils extends SessionUtils {

    @Override
    public void restoreUserToSandbox(UserSandbox sandbox, SessionWrapper session, CommCarePlatform platform,
            String username, String password, PrintStream printStream) {
    }

    @Override
    public int doPostRequest(PostRequest syncPost, SessionWrapper session, String username,
            String password, PrintStream printStream) throws IOException {
        return 201;
    }
}
