package org.commcare.util.test;

import org.commcare.api.persistence.SqlSandbox;
import org.commcare.api.util.UserDataUtils;
import org.commcare.cases.model.Case;
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
        sandbox = UserDataUtils.getStaticStorage(username);
        UserDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/ipm_restore.xml"), sandbox);
    }

    @Test
    public void test() {
        Case readCase = sandbox.getCaseStorage().read(1);
        System.out.println("read case: " + readCase + " name: " + readCase.getTypeId());
    }
}
