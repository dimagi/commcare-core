package org.commcare.applogic;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.model.PeriodicWrapperState;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.log.activity.DeviceReportState;
import org.javarosa.log.properties.LogPropertyRules;
import org.javarosa.service.transport.securehttp.DefaultHttpCredentialProvider;
import org.javarosa.user.api.CreateUserController;
import org.javarosa.user.api.LoginController;
import org.javarosa.user.api.LoginState;
import org.javarosa.user.model.User;

public class CommCareLoginState extends LoginState {
	boolean interactive;
	
	public CommCareLoginState(boolean interactive) {
		this.interactive = interactive;
	}
	
	public CommCareLoginState() {
		this(CommCareContext._().getManager().getCurrentProfile().isFeatureActive("users"));
	}

	public void start() {
		if (interactive) {
			super.start();
		} else {
			User u = getStaticUser();
			loggedIn(u, u.getPassword());
		}
	}

	protected static User getStaticUser() {
		IStorageUtility users = StorageManager.getStorage(User.STORAGE_KEY);
		IStorageIterator ui = users.iterate();
		
		User admin = null;
		while (ui.hasMore()) {
			User u = (User)ui.nextRecord();
			if (u.isAdminUser()) {
				admin = u;
			} else {
				return u;
			}
		}
		return admin;
	}
	
	protected LoginController getController () {		
		String ver = "CommCare " + CommCareUtil.getVersion(CommCareUtil.VERSION_MED);
		String[] extraText = (CommCareUtil.isTestingMode() ? new String[] {ver, "*** TEST BUILD ***"}
											  : new String[] {ver});
		
		String passFormat = PropertyManager._().getSingularProperty(CommCareProperties.PASSWORD_FORMAT);
		
		return new LoginController(
				Localization.get("login.title"),
				PropertyManager._().getSingularProperty(CommCareProperties.LOGIN_IMAGE),
				extraText, CreateUserController.PASSWORD_FORMAT_ALPHA_NUMERIC.equals(passFormat) ? 
				                              CreateUserController.PASSWORD_FORMAT_ALPHA_NUMERIC : 
				                              CreateUserController.PASSWORD_FORMAT_NUMERIC,
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
	public void loggedIn(User u, String password) {
		CommCareContext._().setUser(u, password == null ? null : new DefaultHttpCredentialProvider(u.getUsername(), password));
		Logger.log("login", PropertyUtils.trim(u.getUniqueId(), 8) + "-" + u.getUsername());
		
		CommCareContext._().toggleDemoMode(User.DEMO_USER.equals(u.getUserType()));

		
		//TODO: Replace this state completely with the periodic wrapper state and reimplement this
		//functionality as a periodically wrapped set
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
				J2MEDisplay.startStateWithLoadingScreen(new PeriodicWrapperState(CommCareContext._().getEventDescriptors()){

					public void done() {
						J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());						
					}
				});
			}
		});
	}
}
