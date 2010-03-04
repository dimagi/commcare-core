package org.commcare.applogic;

import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.services.Logger;
import org.javarosa.user.api.LoginController;
import org.javarosa.user.api.LoginState;
import org.javarosa.user.model.User;

public class CommCareLoginState extends LoginState {
	private final static String MIDLET_REMINDERS_PROPERTY = "CommCare-ShowReminders";
	
	protected LoginController getController () {		
		String ver = "CommCare " + CommCareUtil.getVersion(CommCareUtil.VERSION_MED);
		String[] extraText = (CommCareUtil.isTestingMode() ? new String[] {ver, "*** TEST BUILD ***"}
											  : new String[] {ver});
		
		return new LoginController(extraText);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.user.api.transitions.LoginStateTransitions#exit()
	 */
	public void exit() {
		CommCareUtil.exit();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.user.api.transitions.LoginStateTransitions#loggedIn(org.javarosa.user.model.User)
	 */
	public void loggedIn(User u) {
		CommCareContext._().setUser(u);
		Logger.log("login", u.getUserID() + "-" + u.getUsername());
		
		CommCareContext._().toggleDemoMode(User.DEMO_USER.equals(u.getUserType()));

		// Just go to the home state if we're not supposed to do reminders
		new CommCareHomeState().start();
	}
}
