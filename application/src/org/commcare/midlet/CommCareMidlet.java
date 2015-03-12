/**
 *
 */
package org.commcare.midlet;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareStatic;
import org.commcare.util.CommCareUtil;
import org.commcare.util.InitializationListener;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.services.transport.TransportService;

/**
 * @author ctsims
 *
 */
public class CommCareMidlet extends MIDlet {

    /* (non-Javadoc)
     * @see javax.microedition.midlet.MIDlet#destroyApp(boolean)
     */
    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
        Logger.log("shutdown", "Shutting down");
        CommCareStatic.cleanup();
        //We really want to close all of our RMS's to shield them from harm.
        TransportService.halt();
        StorageManager.halt();
        Logger.halt();
        System.out.println("Success in halting resources.");
    }

    /* (non-Javadoc)
     * @see javax.microedition.midlet.MIDlet#pauseApp()
     */
    protected void pauseApp() {

    }

    /* (non-Javadoc)
     * @see javax.microedition.midlet.MIDlet#startApp()
     */
    protected void startApp() throws MIDletStateChangeException {
        CommCareStatic.init();
        CommCareContext.init(this, new InitializationListener() {

            public void onFailure() {
                CommCareUtil.exit();
            }

            public void onSuccess() {
                CommCareUtil.launchFirstState();
            }

        });
    }

}
