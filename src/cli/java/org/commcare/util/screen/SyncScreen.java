package org.commcare.util.screen;

import static org.javarosa.core.model.Constants.EXTRA_POST_SUCCESS;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.PostRequest;

import java.io.IOException;
import java.io.PrintStream;

import datadog.trace.api.Trace;

/**
 * Screen to make a sync request to HQ after a case claim. Unlike other all other screens,
 * SyncScreen does not take input - it simply processes the PostRequest object, makes the request,
 * and displays the result (if necessary)
 */
public class SyncScreen extends Screen {

    protected SessionWrapper sessionWrapper;
    private String username;
    private String password;
    private PrintStream printStream;
    private SessionUtils sessionUtils;
    private boolean syncSuccessful;

    public SyncScreen() {
    }

    public SyncScreen(String username, String password, PrintStream printStream, SessionUtils sessionUtils) {
        super();
        this.username = username;
        this.password = password;
        this.printStream = printStream;
        this.sessionUtils = sessionUtils;
    }

    @Override
    public void init (SessionWrapper sessionWrapper) throws CommCareSessionException {
        this.sessionWrapper = sessionWrapper;
        parseMakeRequest();
    }

    private void parseMakeRequest() throws CommCareSessionException {
        Entry commandEntry = sessionWrapper.getEntryForCommand(sessionWrapper.getCommand());
        PostRequest syncPost = commandEntry.getPostRequest();
        if (syncPost == null) {
            // expected a sync entry; clear session and show vague 'session error' message to user
            throw new CommCareSessionException("Initialized sync request while not on sync screen");
        }

        try {
            int responseCode = sessionUtils.doPostRequest(
                    syncPost, sessionWrapper, username, password, printStream
            );
            syncSuccessful = true;
            if (responseCode != 204) {
                sessionUtils.restoreUserToSandbox(sessionWrapper.getSandbox(),
                    sessionWrapper,
                    sessionWrapper.getPlatform(),
                    username,
                    password,
                    printStream);

                printStream.println("Sync successful with response");
            } else {
                printStream.println("Did not sync because case was already claimed.");
            }
            printStream.println("Press 'enter' to continue.");
        } catch (IOException e) {
            e.printStackTrace();
            printStream.println(String.format("Sync failed with exception %s", e.getMessage()));
            printStream.println("Press 'enter' to retry.");
        }
    }

    @Override
    public boolean prompt(PrintStream printStream) throws CommCareSessionException {
        if (syncSuccessful) {
            printStream.println("Sync complete, press Enter to continue");
        } else {
            printStream.println("Sync failed, press Enter to retry");
        }
        return true;
    }

    @Trace
    @Override
    public boolean handleInputAndUpdateSession(CommCareSession commCareSession, String s, boolean allowAutoLaunch) throws CommCareSessionException {
        if (syncSuccessful) {
            sessionWrapper.addExtraToCurrentFrameStep(EXTRA_POST_SUCCESS, true);
            Entry commandEntry = sessionWrapper.getCurrentEntry();
            if (commandEntry.getXFormNamespace() != null) {
                // session is not complete, keep going
                return true;
            }

            commCareSession.syncState();
            if (commCareSession.finishExecuteAndPop(sessionWrapper.getEvaluationContext())) {
                sessionWrapper.clearVolatiles();
            }
            return true;
        } else {
            parseMakeRequest();
            return false;
        }
    }

    @Override
    public String[] getOptions() {
        return new String[0];
    }
}
