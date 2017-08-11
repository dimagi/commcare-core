package org.commcare.test.utilities;

import org.commcare.cases.util.CaseDBUtils;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.parse.ParseUtils;
import org.commcare.modern.session.SessionWrapper;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by ctsims on 8/11/2017.
 */

public abstract class SyncStateMachine {
    String username;
    String password;
    UserSandbox sandbox;
    SessionWrapper session;
    
    State state;
    State stateBeforeError;
    Strategy strategy;

    String otaFreshRestoreUrl;
    String otaSyncUrl;
    String nextUrlToUse;

    String domain;

    Error error;

    byte[] payload;

    enum State {
        Waiting_For_Init,

        Ready_For_Request,

        Waiting_For_Progress,

        Recovery_Requested,

        Payload_Received,
        Payload_Processed,

        Unrecoverable_Error,
        Recoverable_Error
    }

    enum Strategy {
        Fresh,
        Incremental,
        Recovery
    }

    enum Error {
        Invalid_Credentials,
        Unexpected_Response,
        Network_Error,
        Invalid_Payload
    }

    int unexpectedResponseCode;

    public SyncStateMachine(String username, String password, UserSandbox sandbox,
                                   SessionWrapper session) {
        this.username = username;
        this.password = password;
        this.sandbox = sandbox;
        this.session = session;
    }

    public State getCurrentState() {
        return state;
    }

    public void initialize() throws InvalidStateException {
        if(state != State.Waiting_For_Init) {
            throw new InvalidStateException();
        }

        String urlStateParams = "";

        boolean incremental = false;

        if (sandbox.getLoggedInUser() != null) {
            String syncToken = sandbox.getSyncToken();
            String caseStateHash = CaseDBUtils.computeCaseDbHash(sandbox.getCaseStorage());

            urlStateParams = String.format("&since=%s&state=ccsh:%s", syncToken, caseStateHash);
            incremental = true;

            System.out.println(String.format(
                    "\nIncremental sync requested. \nSync Token: %s\nState Hash: %s",
                    syncToken, caseStateHash));
        }

        //fetch the restore data and set credentials
        otaFreshRestoreUrl = PropertyManager.instance().getSingularProperty("ota-restore-url") +
                "?version=2.0";

        otaSyncUrl = otaFreshRestoreUrl + urlStateParams;

        domain = PropertyManager.instance().getSingularProperty("cc_user_domain");
        final String qualifiedUsername = username + "@" + domain;
        
        setUpAuthentication(qualifiedUsername, password.toCharArray());


        if(incremental) {
            strategy = Strategy.Incremental;
        } else {
            strategy = Strategy.Fresh;
        }

        nextUrlToUse = strategy == Strategy.Fresh ? otaFreshRestoreUrl : otaSyncUrl;

        state = State.Ready_For_Request;
    }

    public abstract void setUpAuthentication(final String qualifiedUsername, final char[] password);
    
    public final void performRequest() throws InvalidStateException {
        if(state != State.Ready_For_Request || state != State.Waiting_For_Progress) {
            throw new InvalidStateException();
        }

        System.out.println(String.format("Request Triggered [Phase: %s]: URL - %s", state.toString(), nextUrlToUse));
        performPlatformRequest(nextUrlToUse);

    }

    protected abstract void performPlatformRequest(String url);

    public void resetFromError() throws InvalidStateException {
        if(state != State.Recoverable_Error) {
            throw new InvalidStateException();
        }
        this.state = stateBeforeError;
        this.stateBeforeError = null;
    }

    protected void unrecoverableError(Error errorCode) {
        this.stateBeforeError = this.state;
        this.state = State.Unrecoverable_Error;

        this.error = errorCode;
    }

    protected void recoverableError(Error errorCode) {
        this.stateBeforeError = this.state;
        this.state = State.Unrecoverable_Error;

        this.error = errorCode;
    }


    public void transitionToRecoveryStrategy() throws InvalidStateException {
        if(state != State.Recovery_Requested) { throw new InvalidStateException(); }

        this.strategy = Strategy.Recovery;
        this.nextUrlToUse = this.otaFreshRestoreUrl;
        this.state = State.Ready_For_Request;
    }

    public final void processPayload() throws InvalidStateException {
        if(state != State.Payload_Received) {
            throw new InvalidStateException();
        }


        processPlatformPayload(strategy == Strategy.Recovery);
    }

    protected abstract void processPlatformPayload(boolean inRecoveryMode);

    public static class InvalidStateException extends Exception {

    }
}
