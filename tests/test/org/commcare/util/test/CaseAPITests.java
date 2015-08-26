package org.commcare.util.test;

import org.commcare.api.persistence.SqlHelper;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;

import static org.junit.Assert.fail;

public class CaseAPITests {

    Case a, b, c, d, e;

    Ledger l;

    DummyIndexedStorageUtility<Case> storage;
    String owner;
    String groupOwner;
    String otherOwner;
    Vector<String> groupOwned;
    Vector<String> userOwned;


    @Before
    public void setUp() throws Exception {

        storage = new DummyIndexedStorageUtility<Case>(Case.class, new PrototypeFactory());

        owner = "owner";
        otherOwner = "otherowner";
        groupOwner = "groupowned";

        userOwned = new Vector<String>();
        userOwned.addElement(owner);

        groupOwned = new Vector<String>();
        groupOwned.addElement(owner);
        groupOwned.addElement(groupOwner);

        a = new Case("123", "a");
        a.setCaseId("a");
        a.setUserId(owner);
        a.setID(12345);
        b = new Case("b", "b");
        b.setCaseId("b");
        b.setUserId(owner);
        c = new Case("c", "c");
        c.setCaseId("c");
        c.setUserId(owner);
        d = new Case("d", "d");
        d.setCaseId("d");
        d.setUserId(owner);
        e = new Case("e", "e");
        e.setCaseId("e");
        e.setUserId(groupOwner);

        l = new Ledger("ledger_entity_id");
        l.setID(12345);
        l.setEntry("test_section_id", "test_entry_id", 2345);
    }

    @Test
    public void testOwnerPurge() {
        try {

            Connection c = null;
            Statement stmt = null;

            try {

                c = DriverManager.getConnection("jdbc:sqlite:test.db");

                SqlHelper.dropTable(c, "TFLedger");

                SqlHelper.createTable(c, "TFLedger", new Ledger());

                SqlHelper.insertToTable(c, "TFLedger", l);

                ResultSet rs = SqlHelper.selectFromTable(c, "TFLedger", new String[]{"entity-id"}, new String[]{"ledger_entity_id"}, new Ledger());
                byte[] caseBytes = rs.getBytes("commcare_sql_record");
                DataInputStream is = new DataInputStream(new ByteArrayInputStream(caseBytes));

                Ledger readLedger = new Ledger();
                try {
                    PrototypeFactory lPrototypeFactory = new PrototypeFactory();
                    lPrototypeFactory.addClass(Ledger.class);
                    readLedger.readExternal(is, lPrototypeFactory);
                } catch(Exception e){
                    System.out.println("e: " + e);
                    e.printStackTrace();
                }


                c.close();

            } catch ( Exception e ) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }
}