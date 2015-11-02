package org.commcare.json;

import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.xml.XmlProcessor;
import org.commcare.test.utilities.MockApp;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 *
 */
public class EvaluateInstanceTest {

    MockApp mApp;

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testEvaluateInstance() throws Exception{
        XmlProcessor mProcessor = new XmlProcessor();
        String response = mProcessor.processRespondXML(new File("/Users/willpride/Dimagi/commcare/tests/resources/xml/test_xpath.xml"));
        System.out.println("Response: " + response);
        Assert.assertEquals(response, "1970-10-23");
    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder();
    }

}
