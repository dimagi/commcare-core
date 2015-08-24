package org.commcare.core.parse;

import org.commcare.core.interfaces.UserDataInterface;
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

    public static void parseIntoSandbox(InputStream stream, UserDataInterface sandbox) throws InvalidStructureException {
        parseIntoSandbox(stream, sandbox, false);
    }

    public static void parseIntoSandbox(InputStream stream, UserDataInterface sandbox, boolean failfast)
            throws InvalidStructureException {
        CommCareTransactionParserFactory factory = new CommCareTransactionParserFactory(sandbox);
        try {
            DataModelPullParser parser = new DataModelPullParser(stream, factory, failfast, true);
            parser.parse();
        } catch (IOException e){
            e.printStackTrace();
        } catch(UnfullfilledRequirementsException e){
            e.printStackTrace();
        } catch(XmlPullParserException e){
            e.printStackTrace();
        }
    }
}
