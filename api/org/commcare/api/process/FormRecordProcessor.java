package org.commcare.api.process;

import org.commcare.api.interfaces.UserDataInterface;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.xml.CaseXmlParser;
import org.commcare.xml.LedgerXmlParsers;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility methods for processing XML transactions against a user sandbox
 *
 * Created by wpride1 on 7/21/15.
 */
public class FormRecordProcessor {

    public static void processXML(UserDataInterface sandbox, String fileText) throws IOException, XmlPullParserException, UnfullfilledRequirementsException, InvalidStructureException {

        try {
            InputStream stream = new ByteArrayInputStream(fileText.getBytes(StandardCharsets.UTF_8));
            process(sandbox, stream);
        }catch(Exception e){
            System.out.println("e1: " + e);
            e.printStackTrace();
        }
    }

    public static void processFile(UserDataInterface sandbox, File record) throws IOException, XmlPullParserException, UnfullfilledRequirementsException, InvalidStructureException {
        try {
            InputStream stream = new FileInputStream(record);
            process(sandbox, stream);
        }catch(Exception e){
            System.out.println("e2: " + e);
            e.printStackTrace();
        }
    }

    public static void process(UserDataInterface sandbox, InputStream stream) throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException, StorageFullException {
        try {
            final UserDataInterface mSandbox = sandbox;

            InputStream is = stream;

            DataModelPullParser parser = new DataModelPullParser(is, new TransactionParserFactory() {
                public TransactionParser getParser(KXmlParser parser) {
                    if (LedgerXmlParsers.STOCK_XML_NAMESPACE.equals(parser.getNamespace())) {
                        return new LedgerXmlParsers(parser, mSandbox.getLedgerStorage());
                    } else if ("case".equalsIgnoreCase(parser.getName())) {
                        return new CaseXmlParser(parser, mSandbox.getCaseStorage());
                    }
                    return null;
                }

            }, true, true);

            parser.parse();
            is.close();
        } catch(Exception e){
            System.out.println("e3: " + e);
            e.printStackTrace();
        }
    }
}
