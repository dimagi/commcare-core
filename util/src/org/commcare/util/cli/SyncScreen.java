package org.commcare.util.cli;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.CommCareSession;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.DisplayUnit;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
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
import java.util.Hashtable;
import java.util.Map;

/**
 * Screen that displays user configurable entry texts and makes
 * a case query to the server with these fields.
 *
 * @author wspride
 */
public class SyncScreen extends Screen {

    SessionWrapper sessionWrapper;

    @Override
    public void init(SessionWrapper sessionWrapper) throws CommCareSessionException {
        this.sessionWrapper = sessionWrapper;
    }

    @Override
    public void prompt(PrintStream printStream) throws CommCareSessionException {

    }

    @Override
    public boolean handleInputAndUpdateSession(CommCareSession commCareSession, String s) throws CommCareSessionException {
        return false;
    }

    @Override
    public String[] getOptions() {
        return new String[0];
    }
}
