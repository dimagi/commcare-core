package org.commcare.view;


import java.util.Date;
import java.util.Vector;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareSense;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.user.model.User;

import de.enough.polish.ui.ChoiceItem;
import de.enough.polish.ui.Command;
import de.enough.polish.ui.List;
import de.enough.polish.ui.UiAccess;

/**
 * @author Clayton Sims
 * @date Jan 23, 2009 
 *
 */
public class CommCareHomeScreen extends List {
	CommCareHomeController controller;

	private boolean isAdmin;
	
	private User loggedInUser;
	
	private boolean reviewEnabled;
	
	public ChoiceItem sendAllUnsent = new ChoiceItem(Localization.get("menu.send.all"), null, List.IMPLICIT);
	public ChoiceItem serverSync = new ChoiceItem(Localization.get("menu.sync"), null, List.IMPLICIT);
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
	public Command admRMSDump = new Command("Dump RMS", Command.ITEM, 1);
	public Command admViewLogs = new Command("View Logs", Command.ITEM, 1);
	public Command admGPRSTest = new Command("Network Test", Command.ITEM, 1);
	public Command adminLogin = new Command("Admin Login", Command.ITEM, 1);
	public Command admForceSend = new Command("Force Send", Command.ITEM, 1);
	
	public CommCareHomeScreen(CommCareHomeController controller, Vector<Suite> suites, User loggedInUser, boolean reviewEnabled) {
		super(Localization.get("homescreen.title"), List.IMPLICIT);
		this.controller = controller;
		
		setCommandListener(controller);
		setSelectCommand(select);
		
		addCommand(exit);
		this.loggedInUser = loggedInUser;
		isAdmin = (loggedInUser == null) ? true : loggedInUser.isAdminUser();
		if (isAdmin) {
			addCommand(admSettings);
			addCommand(admNewUser);
			addCommand(admEditUsers);
			addCommand(admDownload);
			addCommand(admResetDemo);
			addCommand(admUpgrade);
			addCommand(admRMSDump);
			addCommand(admViewLogs);
			addCommand(admGPRSTest);
		}
		if (!CommCareContext._().getManager().getCurrentProfile().isFeatureActive("users") && loggedInUser.isAdminUser()) {
			addCommand(adminLogin);
		}
		
		this.reviewEnabled = reviewEnabled;
	}
	
	public void init() {
		if(reviewEnabled) {
			reviewRecent = new ChoiceItem(Localization.get("commcare.review"), null, List.IMPLICIT);
			append(reviewRecent);
		}

		if (CommCareProperties.TETHER_SYNC.equals(PropertyManager._().getSingularProperty(CommCareProperties.TETHER_MODE))) {
			append(serverSync);
			setSync();
		} else if (!CommCareSense.isAutoSendEnabled()) {
			append(sendAllUnsent);
			setSendUnsent();
		} else if (isAdmin) {
			setSendUnsent();
			addCommand(admForceSend);
		}
	}

	public void setSendUnsent() {
		String numunsent = "error"; 
		int unsent = CommCareUtil.getNumberUnsent();
		numunsent = String.valueOf(unsent);
		
		sendAllUnsent.setText(Localization.get("menu.send.all.val", new String[] {numunsent}));
		
		if(unsent > 10) {
			//#style unsentImportant
			UiAccess.setStyle(sendAllUnsent);
		} else {
			//#style listitem
			UiAccess.setStyle(sendAllUnsent);
		}
		
		admForceSend.setLabel(admForceSend.getLabel() + " (" + numunsent + ")");
		
		//If we're in sense mode, we want to change the title of this screen to reflect the logged in
		//user and # of unsent forms.
		if(CommCareSense.sense()) {
			setTitle(loggedInUser.getUsername() + (unsent > 0 ?  ": " + numunsent : ""));
		}
	}
		
	//TODO: localize me
	public void setSync () {
		String sLastSync = PropertyManager._().getSingularProperty(CommCareProperties.LAST_SYNC_AT);
		
		boolean bad = false;
		
		String message = null;
		if (sLastSync != null) {
			Date lastSync = DateUtils.parseDateTime(sLastSync);
			int secs_ago = (int)((new Date().getTime() - lastSync.getTime()) / 1000);
			if (secs_ago < 0) {
				message = "please update ASAP";
				bad = true;
			} else {
				int days_ago = secs_ago / 86400;
				if (days_ago >= 2) {
					message = "updated " + days_ago + " days ago";
				}
				if(days_ago > 5) {
					bad = true;
				}
			}
		} else {
			message = "please update ASAP";
			bad = true;
		}
			
		int numUnsent = CommCareUtil.getNumberUnsent();
		String sUnsent = (numUnsent > 0 ? numUnsent + " item" + (numUnsent > 1 ? "s" : "") + " to send" : null);
		
		if (message == null) {
			message = sUnsent;
		} else if (sUnsent != null) {
			message += "; " + sUnsent;
		}
		
		if(numUnsent > 10) {
			bad = true;
		}
		
		if (message != null) {
			serverSync.setText(serverSync.getText() + " (" + message + ")");
		}
		
		if(bad) {
			//#style unsentImportant
			UiAccess.setStyle(serverSync);
		} else {
			//#style listitem
			UiAccess.setStyle(serverSync);
		}
	}
		
}
