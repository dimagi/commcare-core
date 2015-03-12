package org.commcare.view;


import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import javax.microedition.lcdui.Image;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareSense;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.j2me.util.media.ImageUtils;
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
public class CommCareHomeScreen extends CommCareListView {
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
    public Command adminLogin = new Command(Localization.get("home.change.user"), Command.ITEM, 1);
    public Command admForceSend = new Command("Force Send", Command.ITEM, 1);
    public Command admPermTest = new Command("Permissions Test", Command.ITEM, 1);
    public Command admValidateMedia = new Command("Validate Media", Command.ITEM, 1);

    private int unsentFormNumberLimit = 10;
    private int unsentFormTimeLimit = 3;

    public CommCareHomeScreen(CommCareHomeController controller, User loggedInUser, boolean reviewEnabled) {
        super(Localization.get("homescreen.title"));

        try {
            String unsentPropertyString = PropertyManager._().getSingularProperty(CommCareProperties.UNSENT_FORM_NUMBER_LIMIT);
            if(unsentPropertyString != null) {
                unsentFormNumberLimit = Integer.parseInt(unsentPropertyString);
            }
        } catch(NumberFormatException nfe) {
            //not a valid integer;
        }


        try {
            String unsentPropertyString = PropertyManager._().getSingularProperty(CommCareProperties.UNSENT_FORM_TIME_LIMIT);
            if(unsentPropertyString != null) {
                unsentFormTimeLimit = Integer.parseInt(unsentPropertyString);
            }
        } catch(NumberFormatException nfe) {
            //not a valid integer;
        }


        this.controller = controller;

        setCommandListener(controller);
        setSelectCommand(select);

        addCommand(exit);
        this.loggedInUser = loggedInUser;
        isAdmin = (loggedInUser == null) ? true : loggedInUser.isAdminUser();
        if (isAdmin) {
            addCommand(admSettings);
            if(CommCareContext._().getManager().getCurrentProfile().isFeatureActive(Profile.FEATURE_USERS)) {
                addCommand(admNewUser);
                if(CommCareProperties.USER_REG_SKIP.equals(PropertyManager._().getSingularProperty(CommCareProperties.USER_REG_TYPE))) {
                    addCommand(admEditUsers);
                }
                addCommand(admDownload);
                addCommand(admResetDemo);
            }
            addCommand(admUpgrade);
            addCommand(admRMSDump);
            addCommand(admViewLogs);
            addCommand(admGPRSTest);
            addCommand(admPermTest);
            addCommand(admValidateMedia);
        }
        if (CommCareSense.isAutoLoginEnabled() && !isAdmin) {
            addCommand(adminLogin);
        }

        this.reviewEnabled = reviewEnabled;
    }

    public void setImage(ChoiceItem ci, String filePath){
        try{
            Image mImage = ImageUtils.getImage(Localization.get(filePath));
            ci.setImage(mImage);
        }
        catch (NoLocalizedTextException e){
            System.out.println("couldn't find file path");
        }
    }

    public void init() {
        if(reviewEnabled) {
            setImage(reviewRecent,"commcare.review.icon");
            reviewRecent = new ChoiceItem(Localization.get("commcare.review"), null, List.IMPLICIT);
            append(reviewRecent);
        }

        //serverSync.setImage(serverImage);

        if (CommCareProperties.TETHER_SYNC.equals(PropertyManager._().getSingularProperty(CommCareProperties.TETHER_MODE))) {
            setImage(serverSync,"server.sync.icon.normal");
            append(serverSync);
            setSync();
        } else if (!CommCareSense.isAutoSendEnabled()) {
            setImage(sendAllUnsent,"send.unsent.icon");
            append(sendAllUnsent);
            setSendUnsent();
        } else {
            setSendUnsent();
            if (isAdmin) {
                addCommand(admForceSend);
            }
        }
    }

    public void setSendUnsent() {

        String sLastSync = PropertyManager._().getSingularProperty(CommCareProperties.LAST_SYNC_AT);

        String numunsent = "error";
        int unsent = CommCareUtil.getNumberUnsent();
        numunsent = String.valueOf(unsent);

        sendAllUnsent.setText(Localization.get("menu.send.all.val", new String[] {numunsent}));

        if(unsent > unsentFormNumberLimit) {
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
            String name = loggedInUser.getUsername();
            if(!CommCareContext._().getManager().getCurrentProfile().isFeatureActive(Profile.FEATURE_USERS)) {
                name = Localization.get("homescreen.title");
            }
            setTitle(name + (unsent > 0 ?  ": " + numunsent : ""));
        }
    }

    public void setSync () {

        String sLastSync = PropertyManager._().getSingularProperty(CommCareProperties.LAST_SYNC_AT);

        boolean bad = false;

        String message = null;
        if (sLastSync != null) {
            Date lastSync = DateUtils.parseDateTime(sLastSync);
            int secs_ago = (int)((new Date().getTime() - lastSync.getTime()) / 1000);
            if (secs_ago < 0) {
                message = Localization.get("menu.sync.prompt");
                bad = true;
            } else {
                int days_ago = secs_ago / 86400;
                if (days_ago >= 2) {
                    message = Localization.get("menu.sync.last",new String[] {String.valueOf(days_ago)} );
                }
                if(days_ago > unsentFormTimeLimit) {
                    bad = true;
                }
            }
        } else {
            message = Localization.get("menu.sync.prompt");
            bad = true;
        }

        int numUnsent = CommCareUtil.getNumberUnsent();

        String sUnsent = null;

        if(numUnsent == 1) {
            sUnsent = Localization.get("menu.sync.unsent.one");
        } else if(numUnsent > 1) {
            sUnsent = Localization.get("menu.sync.unsent.mult", new String[]{String.valueOf(numUnsent)});
        }

        if (message == null) {
            message = sUnsent;
        } else if (sUnsent != null) {
            message += "; " + sUnsent;
        }

        if(numUnsent > unsentFormNumberLimit) {
            bad = true;
        }

        if (message != null) {
            serverSync.setText(serverSync.getText() + " (" + message + ")");
        }

        if(bad) {
            //#style unsentImportant
            UiAccess.setStyle(serverSync);

            setImage(serverSync,"server.sync.icon.warn");
        } else {
            //#style listitem
            UiAccess.setStyle(serverSync);
        }
    }

}
