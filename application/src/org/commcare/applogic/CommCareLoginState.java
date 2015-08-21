package org.commcare.applogic;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.model.PeriodicWrapperState;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareSense;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.log.activity.DeviceReportState;
import org.javarosa.log.properties.LogPropertyRules;
import org.javarosa.service.transport.securehttp.DefaultHttpCredentialProvider;
import org.javarosa.user.api.CreateUserController;
import org.javarosa.user.api.LoginController;
import org.javarosa.user.api.LoginState;
import org.javarosa.core.model.User;

public class CommCareLoginState extends LoginState {
    boolean interactive;

    public CommCareLoginState(boolean interactive) {
        this.interactive = interactive;
    }

    public CommCareLoginState() {
        this(CommCareContext._().getManager().getCurrentProfile().isFeatureActive("users") &&
             (!CommCareSense.isAutoLoginEnabled() ||
             PropertyManager._().getSingularProperty(CommCareProperties.LOGGED_IN_USER) == null));
    }

    public void start() {
        if (interactive) {
            super.start();
        } else {
            User u = getLoggedInUser();
            loggedIn(u, u.getPassword());
        }
    }

    protected static User getLoggedInUser() {
        IStorageUtilityIndexed users = (IStorageUtilityIndexed)StorageManager.getStorage(User.STORAGE_KEY);


        if(CommCareSense.isAutoLoginEnabled()) {
            User user = (User)users.getRecordForValue(User.META_UID, PropertyManager._().getSingularProperty(CommCareProperties.LOGGED_IN_USER));
            return user;
        }

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

    /*
     * (non-Javadoc)
     * @see org.javarosa.user.api.LoginState#getController()
     *
     * Returns a LoginController to handle the CommCare LoginProcess
     *
     * @param CommCareProperties.DEMO_MODE = CommCareProperties.DEMO_DISABLED prevents the user from login in as a demo user.
     *         CommCareUtil.demoEnabled() returns false
     *
     * @param CommCareProperties.LOGIN_IMAGES = CommCareProperties.PROPERTY_YES causes the login screen to use images instead of buttons
     *         CommCareUtil.loginImagesEnabled() returns true
     *
     * @param CommCareProperties.PASSWORD_FORMAT = CreateUserController.PASSWORD_FORMAT_ALPHA_NUMERIC allows letters in the user's password, otherwise only digits
     *
     * @param CommCareProperties.LOGIN_IMAGE - the URI to an image the login screen should use as a banner, null for no banner
     *
     */

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
                                              CommCareUtil.demoEnabled(),
                                              CommCareUtil.loginImagesEnabled());
    }

    /* (non-Javadoc)
     * @see org.javarosa.user.api.transitions.LoginStateTransitions#exit()
     */
    public void exit() {
        CommCareUtil.exit();
    }

    /* (non-Javadoc)
     * @see org.javarosa.user.api.transitions.LoginStateTransitions#loggedIn(org.javarosa.core.model.User)
     */
    public void loggedIn(final User u, String password) {
        CommCareContext._().setUser(u, password == null ? null : new DefaultHttpCredentialProvider(u.getUsername(), password));
        Logger.log("login", PropertyUtils.trim(u.getUniqueId(), 8) + "-" + u.getUsername());

        CommCareContext._().toggleDemoMode(User.DEMO_USER.equals(u.getUserType()));

        if(CommCareSense.isAutoLoginEnabled()) {
            if(User.STANDARD.equals(u.getUserType() )) {
                //We only want to autolog non-admin non-demo users
                //Set the current user to be automatically logged in
                PropertyManager._().setProperty(CommCareProperties.LOGGED_IN_USER, u.getUniqueId());
            }
            //TODO: Do we want to clear the auto-logged-in user if an admin logs in?
        }


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

                //"admin" login criteria (the actual admin user, not a superuser)
                boolean isAdminUser = CommCareUtil.isMagicAdmin(u);

                //Don't run period events if you're logging in in either
                //A) Admin mode (user with username "admin" and superuser permissions)
                //B) Demo mode
                //TODO: Some events might still want to trigger in admin mode?
                if(CommCareContext._().inDemoMode() || isAdminUser) {
                    //The periodic events really aren't relevant for demo data, so just skip straight to the
                    //actual home.
                    J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
                    return;
                }

                // Go to the home state if we're done or if we skip it.
                J2MEDisplay.startStateWithLoadingScreen(new PeriodicWrapperState(CommCareContext._().getEventDescriptors()){

                    public void done() {
                        J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
                    }
                });
            }
        });
    }

    public void tools() {
        J2MEDisplay.startStateWithLoadingScreen(new CommCareToolsState() {
            public void done() {
                J2MEDisplay.startStateWithLoadingScreen(new CommCareLoginState(true));
            }
        });
    }
}
