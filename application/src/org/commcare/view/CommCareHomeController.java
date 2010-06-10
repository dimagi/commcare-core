/**
 * 
 */
package org.commcare.view;

import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import org.commcare.api.transitions.CommCareHomeTransitions;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareContext;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;


/**
 * @author ctsims
 *
 */
public class CommCareHomeController implements HandledCommandListener {
	CommCareHomeTransitions transitions;
	CommCareHomeScreen view;
	Profile profile;
	
	Vector<Suite> suites;
	
	public CommCareHomeController (Vector<Suite> suites, Profile profile) {
		this.suites = suites;
		this.profile = profile;
		boolean admin = false;
		if(!CommCareContext._().getManager().getCurrentProfile().isFeatureActive("users")) {
			admin = true;
		} else {
			admin = CommCareContext._().getUser().isAdminUser();
		}
		view = new CommCareHomeScreen(this, suites, admin, profile.isFeatureActive(Profile.FEATURE_REVIEW));
	}
	
	public void setTransitions (CommCareHomeTransitions transitions) {
		this.transitions = transitions;
	}

	public void start() {
		J2MEDisplay.setView(view);
	}

	public void commandAction(Command c, Displayable d) {
		CrashHandler.commandAction(this, c, d);
	}  

	public void _commandAction(Command c, Displayable d) {
		if (c == view.select) {

			if(view.getCurrentItem() == view.sendAllUnsent) {
				transitions.sendAllUnsent();
			} else if(view.getCurrentItem() == view.reviewRecent) {
				transitions.review();
			} else {
				
				Menu m = view.getSelectedMenu();
				if(m != null) {
					Suite s = view.getSelectedSuite();
					transitions.viewSuite(s, m);
				} else {
					Entry e = view.getSelectedEntry();
					if(e != null) {
						Suite s = view.getSelectedSuite();
						transitions.entry(s,e);
					}
				} 
			}
		} else if (c == view.exit) {
			transitions.logout();
		}else if (c == view.admSettings) {
			transitions.settings();
		} else if (c == view.admNewUser) {
			transitions.newUser();
		} else if (c == view.admEditUsers) {		
			transitions.editUsers();
		} else if (c == view.admDownload) {
			transitions.restoreUserData();
		} else if (c == view.admResetDemo) {
			transitions.resetDemo();
		}else if (c == view.admUpgrade) {
			transitions.upgrade();
		}
	}
}
