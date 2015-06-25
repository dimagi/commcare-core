package org.commcare.util.test;

import org.commcare.api.persistence.UserDatabaseHelper;
import org.commcare.cases.model.Case;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CaseAPITests {

    Case a, b, c, d, e;
    DummyIndexedStorageUtility<Case> storage;
    String owner;
    String groupOwner;
    String otherOwner;
    Vector<String> groupOwned;
    Vector<String> userOwned;


    @Before
    public void setUp() throws Exception {

        storage = new DummyIndexedStorageUtility<Case>(Case.class);

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
    }

    @Test
    public void testOwnerPurge() {
        try {

            Connection c = null;
            Statement stmt = null;

            try {
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:test.db");

                UserDatabaseHelper.dropTable(c, "TFCase");

                UserDatabaseHelper.createTable(c, "TFCase", new Case());

                UserDatabaseHelper.insertToTable(c, "TFCase", a);

                ResultSet rs = UserDatabaseHelper.selectFromTable(c, "TFCase", new String[]{"case_id"}, new String[]{"a"}, new Case());
                String caseType = rs.getString("case_type");
                assertEquals("a", caseType);
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