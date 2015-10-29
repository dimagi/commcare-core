package org.commcare.json;

import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.xml.XmlProcessor;
import org.commcare.test.utilities.MockApp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 *
 */
public class RestoreTests {

    MockApp mApp;

    @Before
    public void setUp() throws Exception {
        //mApp = new MockApp("/session-tests-template/");
    }

    @Test
    public void testRestore() throws Exception{
        XmlProcessor mProcessor = new XmlProcessor();
        mProcessor.processRespondXML(new File("/Users/willpride/Dimagi/commcare/tests/resources/xml/restore.xml"));
    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder();
    }

}
