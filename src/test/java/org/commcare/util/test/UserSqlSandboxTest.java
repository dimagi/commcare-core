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
import static org.junit.Assert.assertTrue;

import java.io.File;

/**
 * Tests for the SqlSandsbox API. Just initializes and makes sure we can access at the moment.
 *
 * @author wspride
 */
public class UserSqlSandboxTest {

    private UserSqlSandbox sandbox;
    private final String username = "sandbox-test-user";

    @Before
    public void setUp() throws Exception {
        sandbox = new UserSqlSandbox(username, UserSqlSandbox.DEFAULT_DATBASE_PATH);
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().getResourceAsStream("ipm_restore.xml"), sandbox);
        sandbox = null;
    }

    @Test
    public void test() {
        sandbox = new UserSqlSandbox(username, UserSqlSandbox.DEFAULT_DATBASE_PATH);
        assertEquals(sandbox.getCaseStorage().getNumRecords(), 6);
        assertEquals(sandbox.getLedgerStorage().getNumRecords(), 3);
        assertEquals(sandbox.getUserFixtureStorage().getNumRecords(), 4);
        User loggedInUser = sandbox.getLoggedInUser();
        assertEquals(loggedInUser.getUsername(), "test");
    }

    @Test
    public void testAlternativePath() throws Exception{
        sandbox = new UserSqlSandbox(username, "alternative-dbs");
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().getResourceAsStream("ipm_restore.xml"), sandbox);
        assertEquals(sandbox.getCaseStorage().getNumRecords(), 6);
        assertEquals(sandbox.getLedgerStorage().getNumRecords(), 3);
        assertEquals(sandbox.getUserFixtureStorage().getNumRecords(), 4);
        File dbFolder = new File("alternative-dbs");
        assertTrue(dbFolder.exists() && dbFolder.isDirectory());
        SqlSandboxUtils.deleteDatabaseFolder("alternative-dbs");
        assertTrue(!dbFolder.exists() && !dbFolder.isDirectory());
    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
        sandbox.closeConnection();
    }
}
