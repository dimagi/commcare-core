package org.commcare.util.cli;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.DisplayUnit;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Screen to allow users to choose items from session menus.
 *
 * @author ctsims
 */
public class QueryScreen extends Screen {

    private RemoteQuerySessionManager remoteQuerySessionManager;
    Hashtable<String, DisplayUnit> userInputDisplays;
    String[] fields;

    String mTitle;

    @Override
    public void init(SessionWrapper sessionWrapper) throws CommCareSessionException {
        remoteQuerySessionManager =
                RemoteQuerySessionManager.buildQuerySessionManager(sessionWrapper,
                        sessionWrapper.getEvaluationContext());
        userInputDisplays = remoteQuerySessionManager.getNeededUserInputDisplays();

        int count = 0;
        fields = new String[userInputDisplays.keySet().size()];
        for (Map.Entry<String, DisplayUnit> displayEntry : userInputDisplays.entrySet()) {
            fields[count] = displayEntry.getValue().getText().evaluate(sessionWrapper.getEvaluationContext());
        }
        mTitle = "Case Claim";

    }

    public String getScreenTitle() {
        return mTitle;
    }


    @Override
    public void prompt(PrintStream out) {

    }

    @Override
    public String[] getOptions() {
        return fields;
    }

    @Override
    public boolean handleInputAndUpdateSession(CommCareSession session, String input) {
        return true;
    }

    public Hashtable<String, DisplayUnit> getUserInputDisplays(){
        return userInputDisplays;
    }
}
