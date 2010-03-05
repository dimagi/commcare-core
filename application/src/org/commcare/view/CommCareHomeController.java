/**
 * 
 */
package org.commcare.view;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import org.commcare.api.transitions.CommCareHomeTransitions;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareContext;
import org.javarosa.cases.util.ICaseType;
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
	
	Vector<Suite> suites;
	
	public CommCareHomeController (Vector<Suite> suites) {
		this.suites = suites;		
		view = new CommCareHomeScreen(this, suites, CommCareContext._().getUser().isAdminUser());
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
			} else {
			
				Enumeration en = suites.elements();
				while(en.hasMoreElements()) {
					Suite suite = (Suite)en.nextElement();
					if(view.getString(view.getCurrentIndex()).equals(suite.getName())) {
						transitions.viewSuite(suite);
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
		} else if (c == view.admBackupRestore) {
			transitions.backupRestore();
		} else if (c == view.admResetDemo) {
			transitions.resetDemo();
		}
	}
}
