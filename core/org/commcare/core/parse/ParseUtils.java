package org.commcare.core.parse;

import org.commcare.core.interfaces.UserDataInterface;
import org.commcare.data.xml.DataModelPullParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by wpride1 on 8/11/15.
 */
public class ParseUtils {
    public static void parseXMLIntoSandbox(String restore, UserDataInterface sandbox) {
        InputStream stream = new ByteArrayInputStream(restore.getBytes(StandardCharsets.UTF_8));
        parseIntoSandbox(stream, sandbox);
    }

    public static void parseFileIntoSandbox(File restore, UserDataInterface sandbox) throws FileNotFoundException {
        InputStream stream = new FileInputStream(restore);
        parseIntoSandbox(stream, sandbox);
    }

    public static void parseIntoSandbox(InputStream stream, UserDataInterface sandbox) {
        CommCareTransactionParserFactory factory = new CommCareTransactionParserFactory(sandbox);
        try {
            DataModelPullParser parser = new DataModelPullParser(stream, factory);
            parser.parse();
            sandbox.updateLastSync();
        } catch (IOException e){
            e.printStackTrace();
        } catch(UnfullfilledRequirementsException e){
            e.printStackTrace();
        } catch(XmlPullParserException e){
            e.printStackTrace();
        } catch(InvalidStructureException e) {
            e.printStackTrace();
        }
    }
}
