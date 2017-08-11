package org.commcare.util.mocks;

import org.commcare.cases.util.CaseDBUtils;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.session.SessionWrapper;
import org.javarosa.core.services.PropertyManager;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by ctsims on 8/11/2017.
 */

public abstract class SyncStateMachine {
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
    String errorMessage;

    byte[] payload;

    //TODO: Error should be typed and come with metadata.
    public int getServerResponseCode() {
        return unexpectedResponseCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public enum State {
        Waiting_For_Init,

        Ready_For_Request,

        Waiting_For_Progress,

        Recovery_Requested,

        Payload_Received,

        Success,

        Unrecoverable_Error,
        Recoverable_Error
    }

    public enum Strategy {
        Fresh,
        Incremental,
        Recovery
    }

    public enum Error {
        Invalid_Credentials,
        Unexpected_Server_Response_Code,
        Network_Error,
        Invalid_Payload,
        Unhandled_Situation
    }

    int unexpectedResponseCode;

    public SyncStateMachine(String username, String password, UserSandbox sandbox,
                                   SessionWrapper session) {
        this.sandbox = sandbox;
        this.session = session;
        this.state = State.Waiting_For_Init;
    }

    public State getCurrentState() {
        return state;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void initialize() throws SyncErrorException {
        if(state != State.Waiting_For_Init) {
            throw getInvalidStateException("Initialize");
        }

        //TODO: Figure out how much of this should be platform;

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

        //TODO: This stuff should migrate out in a restructure to the platform side

        domain = PropertyManager.instance().getSingularProperty("cc_user_domain");

        if(incremental) {
            strategy = Strategy.Incremental;
        } else {
            strategy = Strategy.Fresh;
        }

        nextUrlToUse = strategy == Strategy.Fresh ? otaFreshRestoreUrl : otaSyncUrl;

        state = State.Ready_For_Request;
    }

    protected void populateCurrentPlatformConnectionHeaders(HashMap<String, String> properties) {
        //TODO: This should get centralized around common requests.

        properties.put("X-OpenRosa-Version", "2.1");
        if (sandbox.getSyncToken() != null) {
            properties.put("X-CommCareHQ-LastSyncToken", sandbox.getSyncToken());
        }
        properties.put("x-openrosa-deviceid", "commcare-mock-utility");
    }

    /**
     * @Outcomes: Waiting_For_Progress, Recovery_Requested, Payload_Received
     */
    public final void performRequest() throws SyncErrorException {
        if(state != State.Ready_For_Request && state != State.Waiting_For_Progress) {
            throw getInvalidStateException("Perform Request");
        }

        performPlatformRequest(nextUrlToUse);
    }

    protected abstract void performPlatformRequest(String url);

    /**
     * Incoming States: Waiting_For_Progress
     * Outgoing States: Ready_For_Request
     */
    public final void processWaitSignal() throws SyncErrorException {
        if(state != State.Waiting_For_Progress) {
            throw getInvalidStateException("Waiting for Progress");
        }
        processPlatformWaitSignal();
    }

    protected abstract void processPlatformWaitSignal();


    public void resetFromError() throws SyncErrorException {
        if(state != State.Recoverable_Error) {
            throw getInvalidStateException("Recover from Error");
        }
        this.state = stateBeforeError;
        this.stateBeforeError = null;
    }

    protected void unrecoverableError(Error errorCode) {
        unrecoverableError(null, errorCode);
    }

    protected void unrecoverableError(String message, Error errorCode) {
        this.stateBeforeError = this.state;
        this.errorMessage = message;
        this.state = State.Unrecoverable_Error;

        this.error = errorCode;
    }

    protected void recoverableError(Error errorCode) {
        this.stateBeforeError = this.state;
        this.state = State.Unrecoverable_Error;

        this.error = errorCode;
    }


    public void transitionToRecoveryStrategy() throws SyncErrorException {
        if(state != State.Recovery_Requested) {
            throw getInvalidStateException("Transition to Recovery Mode");
        }

        this.strategy = Strategy.Recovery;
        this.nextUrlToUse = this.otaFreshRestoreUrl;
        this.state = State.Ready_For_Request;
    }

    public final void processPayload() throws SyncErrorException {
        if(state != State.Payload_Received) {
            throw getInvalidStateException("Process Payload");
        }

        processPlatformPayload(strategy == Strategy.Recovery);

        if(this.getCurrentState() != State.Success) {
            throw getInvalidStateException("process payload must succeed or set an unrecoverable error");
        }
    }

    protected abstract void processPlatformPayload(boolean inRecoveryMode);


    protected InvalidStateException getInvalidStateException(String action) throws SyncErrorException {
        if(state == State.Unrecoverable_Error) {
            throw new SyncErrorException(errorMessage, this.stateBeforeError, this.error);
        } else {
            return new InvalidStateException(
                    String.format("Action [%s] is incompatible with the current sync state: %s",
                            action, state));
        }
    }

    public static class SyncErrorException extends Exception {
        SyncStateMachine.State stateBeforeError;
        Error error;

        public SyncErrorException(String message, State stateBeforeError, Error error) {
            super(message);
            this.stateBeforeError = stateBeforeError;
            this.error = error;
        }

        public SyncErrorException(State stateBeforeError, Error error) {
            this(null, stateBeforeError, error);
        }

        public State getStateBeforeError() {
            return stateBeforeError;
        }

        public Error getSyncError() {
            return error;
        }
    }


    public static class InvalidStateException extends RuntimeException {
        public InvalidStateException(String msg) {
            super(msg);
        }
    }


}
