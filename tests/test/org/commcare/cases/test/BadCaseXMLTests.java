package org.commcare.cases.test;

import org.commcare.resources.model.Resource;
import org.commcare.suite.model.Profile;
import org.commcare.test.utils.PersistableSandbox;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.xml.ProfileParser;
import org.javarosa.core.util.ArrayUtilities;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test suite to verify end-to-end parsing of inbound case XML
 * and reading values back from the casedb model
 * 
 * @author ctsims
 */
public class BadCaseXMLTests {

    MockUserDataSandbox sandbox;
    
    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }
    
    @Test
    public void testNoCaseID() {
        //Expected - Fail silently (TODO: Fix?)
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/case_create_broken_no_caseid.xml"), sandbox);

        //Make sure that we didn't make a case entry for the bad case though
        //assertEquals("Case XML with no id should not have created a case record", sandbox.getCaseStorage().getNumRecords(), 0);
    }
}
