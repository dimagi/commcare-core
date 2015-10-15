package org.commcare.core.process;

import org.commcare.core.interfaces.AbstractUserSandbox;
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
 * Utility methods for processing XML transactions against a user sandbox
 *
 * Created by wpride1 on 7/21/15.
 */
public class XmlFormRecordProcessor {

    public static void process(AbstractUserSandbox sandbox, InputStream stream) throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException, StorageFullException {
        final AbstractUserSandbox mSandbox = sandbox;

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
    }
}
