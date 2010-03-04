/**
 * 
 */
package org.commcare.midlet;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import org.commcare.applogic.CommCareLoginState;
import org.commcare.util.CommCareContext;

/**
 * @author ctsims
 *
 */
public class CommCareMidlet extends MIDlet {

	/* (non-Javadoc)
	 * @see javax.microedition.midlet.MIDlet#destroyApp(boolean)
	 */
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {

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
		CommCareContext.init(this);
		new CommCareLoginState().start();
	}

}
