package org.commcare.util.test;

import org.commcare.api.persistence.SqlSandbox;
import org.commcare.api.util.UserDataUtils;
import org.commcare.cases.util.CasePurgeFilter;
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
        UserDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/case_create_purge.xml"), sandbox);
        owners = UserDataUtils.extractEntityOwners(sandbox);
    }

    @Test
    public void test() {
        CasePurgeFilter purger = new CasePurgeFilter(sandbox.getCaseStorage(), owners);
        int removedCases = sandbox.getCaseStorage().removeAll(purger).size();

        if (removedCases == 0) {
            throw new RuntimeException("Failed to remove purged case");
        }

        if (sandbox.getCaseStorage().getNumRecords() < 1) {
            throw new RuntimeException("Incorrectly purged case");
        }

        if (sandbox.getCaseStorage().getNumRecords() > 1) {
            throw new RuntimeException("Incorrectly retained case");
        }
    }
}
