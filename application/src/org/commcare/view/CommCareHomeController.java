/**
 * 
 */
package org.commcare.view;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import org.commcare.api.transitions.CommCareHomeTransitions;
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
	
	Vector<ICaseType> caseTypes;
	
	public CommCareHomeController (Vector<ICaseType> caseTypes) {
		this.caseTypes = caseTypes;		
		view = new CommCareHomeScreen(this, caseTypes, CommCareContext._().getUser().isAdminUser());
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
			Enumeration en = caseTypes.elements();
			while(en.hasMoreElements()) {
				ICaseType type = (ICaseType)en.nextElement();
				if(view.getString(view.getCurrentIndex()).equals(type.getCaseTypeName())) {
					transitions.caseChosen(type);
				}
			}

			if(view.getCurrentItem() == view.sendAllUnsent) {
				transitions.sendAllUnsent();
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
