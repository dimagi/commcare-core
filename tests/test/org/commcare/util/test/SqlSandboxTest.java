package org.commcare.util.test;

import org.commcare.api.models.User;
import org.commcare.api.persistence.SqlSandbox;
import org.commcare.api.util.UserDataUtils;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.javarosa.core.model.instance.FormInstance;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

/**
 * Tests for the SqlSandbox API
 *
 * @author wspride
 */
public class SqlSandboxTest {

    private SqlSandbox sandbox;
    private Vector<String> owners;
    String username = "sandbox-test-user";

    @Before
    public void setUp() throws Exception {
        sandbox = UserDataUtils.getClearedStaticStorage(username);
        System.out.println("get resource as stream: " + this.getClass().getResourceAsStream("ipm_restore.xml"));
        UserDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("ipm_restore.xml"), sandbox);
        sandbox = null;
    }

    @Test
    public void test() {
        sandbox = UserDataUtils.getStaticStorage(username);
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
