package org.commcare.view;


import java.util.Enumeration;
import java.util.Vector;

import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareUtil;
import org.javarosa.cases.util.ICaseType;
import org.javarosa.core.services.locale.Localization;

import de.enough.polish.ui.ChoiceItem;
import de.enough.polish.ui.Command;
import de.enough.polish.ui.List;

/**
 * @author Clayton Sims
 * @date Jan 23, 2009 
 *
 */
public class CommCareHomeScreen extends List {
	CommCareHomeController controller;
	
	public ChoiceItem sendAllUnsent = new ChoiceItem(Localization.get("menu.send.all"), null, List.IMPLICIT);  

	public Command select = new Command("Select", Command.ITEM, 1);
	public Command exit = new Command("Exit", Command.EXIT, 1);
	public Command admNewUser = new Command("New User", Command.ITEM, 1);
	public Command admSettings = new Command("Settings", Command.ITEM, 1);
	public Command admFeedbackReport = new Command("Report Feedback", Command.ITEM, 1);
	public Command admEditUsers = new Command("Edit Users", Command.ITEM, 1);
	public Command admDeletePatient = new Command("Delete Patient", Command.ITEM, 1);
	public Command admBackupRestore = new Command("Backup/Restore", Command.ITEM, 1);
	public Command admJunkInDaTrunk = new Command("Generate Junk", Command.ITEM, 1);
	public Command admResetDemo = new Command("Reset Demo", Command.ITEM, 1);

	public CommCareHomeScreen(CommCareHomeController controller, Vector<Suite> suites, boolean adminMode) {
		super("CommCare", List.IMPLICIT);
		this.controller = controller;
		
		Enumeration en = suites.elements();
		while(en.hasMoreElements()) {
			Suite suite = (Suite)en.nextElement();
			append(suite.getName(), null);
		}

		append(sendAllUnsent);
		setSendUnsent();

		setCommandListener(controller);
		setSelectCommand(select);
		
		addCommand(exit);
		if (adminMode) {
			addCommand(admSettings);
			addCommand(admNewUser);
			addCommand(admEditUsers);
			addCommand(admBackupRestore);
			addCommand(admResetDemo);
		}
	}
	

	public void setSendUnsent() {
		String numunsent = "error"; 
		numunsent = String.valueOf(CommCareUtil.getNumberUnsent());
		sendAllUnsent.setText(Localization.get("menu.send.all.val", new String[] {numunsent}));
	}

}
