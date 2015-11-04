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
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 *
 */
public class JsonApiTests {

    MockApp mApp;
    XmlProcessor mProcessor;

    @Before
    public void setUp() throws Exception {
        mProcessor = new XmlProcessor();
    }

    @Test
    public void testEvaluateInstance() throws Exception{
        String response = mProcessor.processRespondXML(new File("/Users/willpride/Dimagi/commcare/tests/resources/xml/get_form_json.xml"));
        System.out.println("response: " + response);
    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder();
    }

}
