/**
 *
 */
package org.commcare.applogic;

import org.commcare.api.transitions.FirstStartupTransitions;
import org.commcare.core.properties.CommCareProperties;
import org.commcare.util.CommCareContext;
import org.commcare.view.FirstStartupView;
import org.javarosa.core.api.State;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.core.model.User;

/**
 * @author ctsims
 *
 */
public class CommCareFirstStartState implements State, FirstStartupTransitions{

    FirstStartupView view;

    public CommCareFirstStartState() {
        view = new FirstStartupView(this);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.api.State#start()
     */
    public void start() {
        J2MEDisplay.setView(view);
    }

    public void exit() {
        CommCareContext._().exitApp();
    }

    public void login() {
        J2MEDisplay.startStateWithLoadingScreen(new CommCareLoginState() {

            public void loggedIn(User u, String password) {

                //If they logged in as an admin user, we can assume they don't need the first start screen anymore.
                if(!(u.getUserType().equals(User.DEMO_USER))) {
                    PropertyManager._().setProperty(CommCareProperties.IS_FIRST_RUN, CommCareProperties.PROPERTY_NO);
                }

                super.loggedIn(u, password);
            }

        });
    }

    public void restore() {
        J2MEDisplay.startStateWithLoadingScreen(new CommCareOTARestoreState() {

            public void cancel() {
                J2MEDisplay.startStateWithLoadingScreen(new CommCareFirstStartState());
            }

            public void done(boolean errorsOccurred) {
                PropertyManager._().setProperty(CommCareProperties.IS_FIRST_RUN, CommCareProperties.PROPERTY_NO);
                J2MEDisplay.startStateWithLoadingScreen(new CommCareLoginState());
            }

            public void commitSyncToken(String restoreID) {
                //Will be set on any created users if relevant.
            }

        });
    }

}
