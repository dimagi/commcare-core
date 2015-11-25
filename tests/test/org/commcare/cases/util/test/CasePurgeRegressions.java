package org.commcare.cases.util.test;

import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.core.parse.ParseUtils;
import org.commcare.core.sandbox.SandboxUtils;
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
public class CasePurgeRegressions {

    @Test
    public void testSimpleExtensions() throws Exception {
        MockUserDataSandbox sandbox;
        Vector<String> owners;
        sandbox = MockDataUtils.getStaticStorage();

        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().getResourceAsStream("case_purge/simple_extension_test.xml"), sandbox);
        owners = SandboxUtils.extractEntityOwners(sandbox);

        CasePurgeFilter purger = new CasePurgeFilter(sandbox.getCaseStorage(), owners);
        int removedCases = sandbox.getCaseStorage().removeAll(purger).size();

        if (removedCases > 0) {
            throw new RuntimeException("Incorrectly removed cases");
        }
    }
}
