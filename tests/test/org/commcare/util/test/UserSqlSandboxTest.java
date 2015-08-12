package org.commcare.util.test;

import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.core.parse.ParseUtils;
import org.commcare.suite.model.User;
import org.javarosa.core.model.instance.FormInstance;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

/**
 * Tests for the SqlSandbox API
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
        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().getResourceAsStream("ipm_restore.xml"), sandbox);
        sandbox = null;
    }

    @Test
    public void test() {
        sandbox = SqlSandboxUtils.getStaticStorage(username);
        Case readCase = sandbox.getCaseStorage().read(1);
        System.out.println("read case: " + readCase + " name: " + readCase.getTypeId());

        Ledger readLedger = sandbox.getLedgerStorage().read(1);
        System.out.println("read ledger: " + readLedger + " entity id: " + readLedger.getEntiyId());

        FormInstance readFixture = sandbox.getUserFixtureStorage().read(1);
        System.out.println("read fixture: " + readFixture.getRoot());

        User readUser = sandbox.getUserStorage().read(1);
        System.out.println("read user: " + readUser + " name: " + readUser.getUsername());
    }
}
