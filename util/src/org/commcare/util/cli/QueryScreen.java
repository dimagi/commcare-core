package org.commcare.util.cli;

import org.commcare.core.network.ModernHttpRequester;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.CommCareSession;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.DisplayUnit;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Screen to allow users to choose items from session menus.
 *
 * @author ctsims
 */
public class QueryScreen extends Screen {

    protected RemoteQuerySessionManager remoteQuerySessionManager;
    Hashtable<String, DisplayUnit> userInputDisplays;
    SessionWrapper sessionWrapper;
    String[] fields;
    String mTitle;

    @Override
    public void init(SessionWrapper sessionWrapper) throws CommCareSessionException {
        this.sessionWrapper = sessionWrapper;
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

    public void processSuccess(InputStream responseData) {
        Pair<ExternalDataInstance, String> instanceOrError =
                buildExternalDataInstance(responseData,
                        remoteQuerySessionManager.getStorageInstanceName());
        if (instanceOrError.first == null) {
            throw new RuntimeException("Query response format error: " + instanceOrError.second);
        } else if (isResponseEmpty(instanceOrError.first)) {
            throw new RuntimeException("Query response was empty");
        } else {
            sessionWrapper.setQueryDatum(instanceOrError.first);
        }
    }

    /**
     * @return Data instance built from xml stream or the error message raised during parsing
     */
    public static Pair<ExternalDataInstance, String> buildExternalDataInstance(InputStream instanceStream,
                                                                               String instanceId) {
        TreeElement root;
        try {
            KXmlParser baseParser = ElementParser.instantiateParser(instanceStream);
            root = new TreeElementParser(baseParser, 0, instanceId).parse();
        } catch (InvalidStructureException | IOException
                | XmlPullParserException | UnfullfilledRequirementsException e) {
            return new Pair<>(null, e.getMessage());
        }
        return new Pair<>(ExternalDataInstance.buildFromRemote(instanceId, root), "");
    }

    private boolean isResponseEmpty(ExternalDataInstance instance) {
        return !instance.getRoot().hasChildren();
    }

    public void answerPrompts(Hashtable<String, String> answers) {
        for(String key: answers.keySet()){
            remoteQuerySessionManager.answerUserPrompt(key, answers.get(key));
        }
    }

    public URL getBaseUrl(){
        return remoteQuerySessionManager.getBaseUrl();
    }

    public Hashtable<String, String> getQueryParams(){
        return remoteQuerySessionManager.getRawQueryParams();
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
