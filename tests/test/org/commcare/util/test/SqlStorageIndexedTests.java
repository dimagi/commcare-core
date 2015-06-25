package org.commcare.util.test;

import org.commcare.api.persistence.SqlIndexedStorageUtility;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SqlStorageIndexedTests {

    Case a, b, c, d, e;

    Ledger l, l2, l3;

    SqlIndexedStorageUtility<Case> caseStorage;
    SqlIndexedStorageUtility<Ledger> ledgerStorage;
    String owner;
    String groupOwner;
    String otherOwner;
    Vector<String> groupOwned;
    Vector<String> userOwned;


    @Before
    public void setUp() throws Exception {

        owner = "owner";
        otherOwner = "otherowner";
        groupOwner = "groupowned";

        userOwned = new Vector<String>();
        userOwned.addElement(owner);

        groupOwned = new Vector<String>();
        groupOwned.addElement(owner);
        groupOwned.addElement(groupOwner);

        a = new Case("case_name_ipsum", "case_type_ipsum");
        a.setCaseId("case_id_ipsum");
        a.setUserId(owner);
        a.setID(123456);

        l = new Ledger("ledger_entity_id");
        l.setID(12345);
        l.setEntry("test_section_id", "test_entry_id", 2345);

        l2 = new Ledger("ledger_entity_id");
        l2.setID(1234567);
        l2.setEntry("test_section_id_2", "test_entry_id_2", 2345);

        l3 = new Ledger("ledger_entity_id_3");
        l3.setID(12345678);
        l3.setEntry("test_section_id_3", "test_entry_id_3", 2345);
    }

    @Test
    public void testSqlCaseStorage() {
        try {

            try {

                PrototypeFactory mPrototypeFactory = new PrototypeFactory();
                mPrototypeFactory.addClass(Case.class);

                String storageKey = "tfcase";
                String username = "wspride";

                caseStorage = new SqlIndexedStorageUtility<Case>(Case.class, mPrototypeFactory, username, storageKey);

                caseStorage.write(a);

                Case readCase = caseStorage.read(123456);

                assertEquals("case_name_ipsum", readCase.getName());

            } catch ( Exception e ) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
                System.exit(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    @Test
    public void testSqlLedgerStorage() {
        try {

            try {

                PrototypeFactory mPrototypeFactory = new PrototypeFactory();
                mPrototypeFactory.addClass(Ledger.class);

                String storageKey = "tfledger";
                String username = "wspride";

                ledgerStorage = new SqlIndexedStorageUtility<Ledger>(Ledger.class, mPrototypeFactory, username, storageKey);

                ledgerStorage.write(l);
                ledgerStorage.write(l2);
                ledgerStorage.write(l3);

                Ledger readLedger = ledgerStorage.read(12345);

                assertEquals("ledger_entity_id", readLedger.getEntiyId());
                assertEquals("test_section_id", readLedger.getSectionList()[0]);
                assertEquals(2345, readLedger.getEntry("test_section_id", "test_entry_id"));

                Vector<Object> ids = ledgerStorage.getIDsForValue("entity_id", "ledger_entity_id");

                assertEquals(2, ids.size());
                assertTrue(ids.contains(12345));
                assertTrue(ids.contains(1234567));

                Ledger readLedger2 = ledgerStorage.getRecordForValue("entity_id", "ledger_entity_id_3");

                int count = ledgerStorage.getNumRecords();

                assertEquals(count, 3);

                assertTrue(ledgerStorage.exists(12345));
                assertFalse(ledgerStorage.exists(-123));



            } catch ( Exception e ) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
                System.exit(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }
}