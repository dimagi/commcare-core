package org.commcare.cases.util.test;

import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.core.parse.ParseUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

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

        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().getResourceAsStream("case_create_purge.xml"), sandbox);
        owners = CasePurgeRegressions.extractEntityOwners(sandbox);
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
        
        if(!"sync_token_a".equals(sandbox.getSyncToken())) {
            throw new RuntimeException("Invalid Sync Token: " + sandbox.getSyncToken());
        }
    }
}
