package org.commcare.applogic;

import java.io.IOException;
import java.io.InputStream;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.log.WrappedException;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.log.activity.DeviceReportState;
import org.javarosa.log.properties.LogPropertyRules;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;
import org.javarosa.user.api.AddUserController;
import org.javarosa.user.api.LoginController;
import org.javarosa.user.api.LoginState;
import org.javarosa.user.model.User;

public class CommCareLoginState extends LoginState {
	private final static String MIDLET_REMINDERS_PROPERTY = "CommCare-ShowReminders";
	
	protected LoginController getController () {		
		String ver = "CommCare " + CommCareUtil.getVersion(CommCareUtil.VERSION_MED);
		String[] extraText = (CommCareUtil.isTestingMode() ? new String[] {ver, "*** TEST BUILD ***"}
											  : new String[] {ver});
		
		String passFormat = PropertyManager._().getSingularProperty(CommCareProperties.PASSWORD_FORMAT);
		
		return new LoginController(
				Localization.get("login.title"),
				PropertyManager._().getSingularProperty(CommCareProperties.LOGIN_IMAGE),
				extraText, AddUserController.PASSWORD_FORMAT_ALPHA_NUMERIC.equals(passFormat) ? 
				                              AddUserController.PASSWORD_FORMAT_ALPHA_NUMERIC : 
				                              AddUserController.PASSWORD_FORMAT_NUMERIC,
				                              CommCareUtil.demoEnabled());
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
		Logger.log("login", PropertyUtils.trim(u.getUniqueId(), 8) + "-" + u.getUsername());
		
		CommCareContext._().toggleDemoMode(User.DEMO_USER.equals(u.getUserType()));

		J2MEDisplay.startStateWithLoadingScreen(new DeviceReportState() {

			public String getDestURL() {
				String url = PropertyManager._().getSingularProperty(LogPropertyRules.LOG_SUBMIT_URL);
				if(url == null) {
					url = CommCareContext._().getSubmitURL();
				}
				return url;
			}

			public void done() {
				// Go to the home state if we're done or if we skip it.
				J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
			}
		});
	}
}
