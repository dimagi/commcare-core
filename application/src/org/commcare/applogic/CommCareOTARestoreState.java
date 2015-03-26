/**
 *
 */
package org.commcare.applogic;

import org.commcare.restore.CommCareOTARestoreController;
import org.commcare.restore.CommCareOTARestoreTransitions;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareSense;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.api.State;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;

/**
 * @author ctsims
 *
 */
public abstract class CommCareOTARestoreState implements State, CommCareOTARestoreTransitions {

    CommCareOTARestoreController controller;

    private boolean isSync;
    private boolean partial;
    private String syncToken;
    private HttpAuthenticator authenticator;
    private String sampleUsername;

    public CommCareOTARestoreState() {
        this(null, null, null);
        isSync = false;
    }

    /*
     * For a sync state
     */
    public CommCareOTARestoreState(String syncToken, HttpAuthenticator authenticator, String sampleUsername) {
        this.isSync = true;
        this.syncToken = syncToken;
        this.authenticator = authenticator;
        this.partial = getPartialRestoreSetting();
        this.sampleUsername = sampleUsername;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.api.State#start()
     */
    public void start() {
        controller = getController();
        controller.start();
    }

    protected CommCareOTARestoreController getController() {
        return new CommCareOTARestoreController(
            this,
            CommCareContext._().getOTAURL(),
            authenticator,
            isSync,
            !partial,
            syncToken,
            CommCareContext._().getSubmitURL(),
            CommCareSense.sense(),
            sampleUsername
        );
    }

    private boolean getPartialRestoreSetting () {
        return CommCareUtil.partialRestoreEnabled();
    }
}
