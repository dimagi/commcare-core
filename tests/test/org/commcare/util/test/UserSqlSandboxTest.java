package org.commcare.util.test;

import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.core.parse.ParseUtils;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.User;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.util.Vector;

/**
 * Tests for the SqlSandbox API. Just initializes and makes sure we can access at the moment.
 *
 * @author wspride
 */
public class UserSqlSandboxTest {

    private UserSqlSandbox sandbox;
    private Vector<String> owners;
    String username = "sandbox-test-user";

    @Before
    public void setUp() throws Exception {
        sandbox = SqlSandboxUtils.getStaticStorage(username);
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().getResourceAsStream("ipm_restore.xml"), sandbox);
        sandbox = null;
    }

    @Test
    public void test() {
        sandbox = SqlSandboxUtils.getStaticStorage(username);
        assertEquals(sandbox.getCaseStorage().getNumRecords(), 6);
        assertEquals(sandbox.getLedgerStorage().getNumRecords(), 3);
        assertEquals(sandbox.getUserFixtureStorage().getNumRecords(), 4);
        User loggedInUser = sandbox.getLoggedInUser();
        assertEquals(loggedInUser.getUsername(), "test");
    }


    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder();
    }
}
