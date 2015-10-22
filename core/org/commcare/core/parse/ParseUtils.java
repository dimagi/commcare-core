package org.commcare.core.parse;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.data.xml.DataModelPullParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wpride1 on 8/11/15.
 */
public class ParseUtils {

    public static void parseIntoSandbox(InputStream stream, UserSandbox sandbox)
            throws InvalidStructureException, UnfullfilledRequirementsException, XmlPullParserException, IOException {
        parseIntoSandbox(stream, sandbox, false);
    }

    public static void parseIntoSandbox(InputStream stream, UserSandbox sandbox, boolean failfast)
            throws InvalidStructureException, IOException, UnfullfilledRequirementsException, XmlPullParserException {
        CommCareTransactionParserFactory factory = new CommCareTransactionParserFactory(sandbox);
        DataModelPullParser parser = new DataModelPullParser(stream, factory, failfast, true);
        parser.parse();
    }
}
