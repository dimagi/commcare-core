package org.commcare.cases.ledger.test;

import org.commcare.test.utils.TestLedgerInitializer;
import org.commcare.test.utils.XmlComparator;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.model.xform.DataModelSerializer;
import org.junit.Before;
import org.junit.Test;
import org.kxml2.kdom.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class LedgerParseAndReadTest {
    private MockUserDataSandbox sandbox;

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }

    @Test
    public void readLedger() {
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/ledger_create_basic.xml"), sandbox);

        byte[] parsedDb = dumpLedger("jr://instance/ledgerdb");
        Document parsed = XmlComparator.getDocumentFromStream(new ByteArrayInputStream(parsedDb));


    }

    private byte[] dumpLedger(String instanceRef) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataModelSerializer s = new DataModelSerializer(bos, new TestLedgerInitializer(sandbox));

            s.serialize(new ExternalDataInstance(instanceRef, "ledger"), null);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
