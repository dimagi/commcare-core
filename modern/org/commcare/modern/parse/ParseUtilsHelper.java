package org.commcare.modern.parse;

import org.commcare.core.interfaces.UserDataInterface;
import org.commcare.core.parse.ParseUtils;
import org.javarosa.xml.util.InvalidStructureException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Convenience methods, mostly for touchforms so we don't have to deal with Java IO
 * in Jython which is terrible
 *
 * Created by wpride1 on 8/20/15.
 */
public class ParseUtilsHelper  extends ParseUtils {
    public static void parseXMLIntoSandbox(String restore, UserDataInterface sandbox)
            throws InvalidStructureException {
        InputStream stream = new ByteArrayInputStream(restore.getBytes(StandardCharsets.UTF_8));
        parseIntoSandbox(stream, sandbox);
    }

    public static void parseFileIntoSandbox(File restore, UserDataInterface sandbox)
            throws FileNotFoundException, InvalidStructureException {
        InputStream stream = new FileInputStream(restore);
        parseIntoSandbox(stream, sandbox);
    }
}
