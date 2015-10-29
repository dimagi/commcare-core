package org.commcare.util.test;

import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.core.parse.ParseUtils;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.util.externalizable.MD5Hasher;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

/**
 * Tests for the SqlSandsbox API. Just initializes and makes sure we can access at the moment.
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
        Case readCase = sandbox.getCaseStorage().read(1);
        Ledger readLedger = sandbox.getLedgerStorage().read(1);
        FormInstance readFixture = sandbox.getUserFixtureStorage().read(1);
        User readUser = sandbox.getUserStorage().read(1);
    }


    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder();
    }
}
