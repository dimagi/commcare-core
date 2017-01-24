package org.commcare.modern.parse;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.parse.ParseUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Convenience methods, mostly for touchforms so we don't have to deal with Java IO
 * in Jython which is terrible
 *
 * Used by touchforms
 *
 * Created by wpride1 on 8/20/15.
 */
@SuppressWarnings("unused")
public class ParseUtilsHelper  extends ParseUtils {
    public static void parseXMLIntoSandbox(String restore, UserSandbox sandbox, boolean failFast)
            throws InvalidStructureException, UnfullfilledRequirementsException, XmlPullParserException, IOException {
        InputStream stream = new ByteArrayInputStream(restore.getBytes(StandardCharsets.UTF_8));
        parseIntoSandbox(stream, sandbox, failFast);
    }

    public static void parseFileIntoSandbox(File restore, UserSandbox sandbox)
            throws IOException, InvalidStructureException, UnfullfilledRequirementsException, XmlPullParserException {
        InputStream stream = new FileInputStream(restore);
        parseIntoSandbox(stream, sandbox);
    }
}
