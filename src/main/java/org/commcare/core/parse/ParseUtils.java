package org.commcare.core.parse;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.data.xml.DataModelPullParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Created by wpride1 on 8/11/15.
 */
public class ParseUtils {

    public static void parseXMLIntoSandbox(String restore, CommCareTransactionParserFactory factory)
            throws InvalidStructureException, UnfullfilledRequirementsException, XmlPullParserException, IOException {
        InputStream stream = new ByteArrayInputStream(restore.getBytes(StandardCharsets.UTF_8));
        parseIntoSandbox(stream, false, factory);
    }

    public static void parseFileIntoSandbox(File restore, UserSandbox sandbox)
            throws IOException, InvalidStructureException, UnfullfilledRequirementsException, XmlPullParserException {
        InputStream stream = new FileInputStream(restore);
        parseIntoSandbox(stream, sandbox);
    }

    public static void parseIntoSandbox(InputStream stream, UserSandbox sandbox)
            throws InvalidStructureException, UnfullfilledRequirementsException, XmlPullParserException, IOException {
        parseIntoSandbox(stream, sandbox, false);
    }

    public static void parseIntoSandbox(InputStream stream,
                                        boolean failfast,
                                        CommCareTransactionParserFactory factory)
            throws InvalidStructureException, IOException, UnfullfilledRequirementsException, XmlPullParserException {
        DataModelPullParser parser = new DataModelPullParser(stream, factory, failfast, true);
        parser.parse();
    }

    public static void parseIntoSandbox(InputStream stream, UserSandbox sandbox, boolean failfast)
            throws InvalidStructureException, IOException, UnfullfilledRequirementsException, XmlPullParserException {
        CommCareTransactionParserFactory factory = new CommCareTransactionParserFactory(sandbox);
        parseIntoSandbox(stream, failfast, factory);
    }
}
