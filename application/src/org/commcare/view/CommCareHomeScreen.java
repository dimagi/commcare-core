package org.commcare.view;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.utilities.media.MediaUtils;

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
	public ChoiceItem reviewRecent;

	public Command select = new Command(Localization.get("polish.command.select"), Command.ITEM, 1);
	public Command exit = new Command(Localization.get("polish.command.exit"), Command.EXIT, 1);
	public Command admNewUser = new Command(Localization.get("home.user.new"), Command.ITEM, 1);
	public Command admSettings = new Command(Localization.get("home.setttings"), Command.ITEM, 1);
	public Command admFeedbackReport = new Command("Report Feedback", Command.ITEM, 1);
	public Command admEditUsers = new Command(Localization.get("home.user.edit"), Command.ITEM, 1);
	public Command admDeletePatient = new Command("Delete Patient", Command.ITEM, 1);
	public Command admDownload = new Command(Localization.get("home.data.restore"), Command.ITEM, 1);
	public Command admJunkInDaTrunk = new Command("Generate Junk", Command.ITEM, 1);
	public Command admResetDemo = new Command(Localization.get("home.demo.reset"), Command.ITEM, 1);
	public Command admUpgrade = new Command(Localization.get("home.updates"), Command.ITEM, 1);

	public CommCareHomeScreen(CommCareHomeController controller, Vector<Suite> suites, boolean adminMode, boolean reviewEnabled) {
		super(Localization.get("homescreen.title"), List.IMPLICIT);
		this.controller = controller;
				 
		if(reviewEnabled) {
			reviewRecent = new ChoiceItem(Localization.get("commcare.review"), null, List.IMPLICIT);
			append(reviewRecent);
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
			addCommand(admDownload);
			addCommand(admResetDemo);
			addCommand(admUpgrade);
		}
	}

	public void setSendUnsent() {
		String numunsent = "error"; 
		numunsent = String.valueOf(CommCareUtil.getNumberUnsent());
		sendAllUnsent.setText(Localization.get("menu.send.all.val", new String[] {numunsent}));
	}
}
