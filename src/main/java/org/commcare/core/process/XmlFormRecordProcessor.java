package org.commcare.core.process;

import org.commcare.core.interfaces.UserSandbox;
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

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods for processing XML transactions against a user sandbox.
 * This was written to make TouchForms XML submissions easier to perform. Only processes
 * blocks that need to be transacted against the user record (IE cases and ledgers at the moment).
 * This should be used when you have a raw input stream of the XML; FormRecordProcessor on Android
 * should be used when you have a FormRecord object
 *
 * Created by wpride1 on 7/21/15.
 */
public class XmlFormRecordProcessor {

    public static void process(final UserSandbox sandbox, InputStream stream) throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException, StorageFullException {
        process(stream, new TransactionParserFactory() {
            @Override
            public TransactionParser getParser(KXmlParser parser) {
                if (LedgerXmlParsers.STOCK_XML_NAMESPACE.equals(parser.getNamespace())) {
                    return new LedgerXmlParsers(parser, sandbox.getLedgerStorage());
                } else if ("case".equalsIgnoreCase(parser.getName())) {
                    return new CaseXmlParser(parser, sandbox.getCaseStorage());
                }
                return null;
            }

        });

    }

    public static void process(InputStream stream, TransactionParserFactory factory) throws InvalidStructureException,
            IOException, XmlPullParserException, UnfullfilledRequirementsException, StorageFullException {
        DataModelPullParser parser = new DataModelPullParser(stream, factory, true, true);
        parser.parse();
    }
}
