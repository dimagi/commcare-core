package org.commcare.util.test;

import java.util.Vector;

import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.junit.Before;
import org.junit.Test;

/**
 * Quick test to be able to restore a set of user data
 * and ensure users and groups are properly being included
 * in case purges.
 *
 * @author ctsims
 */
public class CasePurgeIntegrationTest {

    private MockUserDataSandbox sandbox;
    private Vector<String> owners;

    @Before
    public void setUp() throws Exception {
        sandbox = MockDataUtils.getStaticStorage();

        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/case_create_purge.xml"), sandbox);
        owners = MockDataUtils.extractEntityOwners(sandbox);
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
