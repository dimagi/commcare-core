package org.commcare.api.process;

import org.commcare.api.persistence.SqlSandbox;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wpride1 on 7/21/15.
 */
public class FormRecordProcessor {

    public static void process(SqlSandbox sandbox, File record) throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException, StorageFullException {

        final File f = record;
        final SqlSandbox mSandbox = sandbox;

        InputStream is = new FileInputStream(f);

        DataModelPullParser parser = new DataModelPullParser(is, new TransactionParserFactory() {
            public TransactionParser getParser(KXmlParser parser) {
                if (LedgerXmlParsers.STOCK_XML_NAMESPACE.equals(parser.getNamespace())) {
                    return new LedgerXmlParsers(parser, mSandbox.getLedgerStorage());
                } else if("case".equalsIgnoreCase(parser.getName())) {
                    return new CaseXmlParser(parser, mSandbox.getCaseStorage());
                }
                return null;
            }

        }, true, true);

        parser.parse();
        is.close();
    }
}
