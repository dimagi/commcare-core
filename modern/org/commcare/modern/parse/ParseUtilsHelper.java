package org.commcare.modern.parse;

import org.commcare.core.interfaces.UserDataInterface;
import org.commcare.core.parse.ParseUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by wpride1 on 8/20/15.
 */
public class ParseUtilsHelper  extends ParseUtils {
    public static void parseXMLIntoSandbox(String restore, UserDataInterface sandbox) {
        InputStream stream = new ByteArrayInputStream(restore.getBytes(StandardCharsets.UTF_8));
        parseIntoSandbox(stream, sandbox);
    }

    public static void parseFileIntoSandbox(File restore, UserDataInterface sandbox) throws FileNotFoundException {
        InputStream stream = new FileInputStream(restore);
        parseIntoSandbox(stream, sandbox);
    }
}
