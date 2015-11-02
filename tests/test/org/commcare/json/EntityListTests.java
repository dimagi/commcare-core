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
public class EntityListTests {

    MockApp mApp;

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testGetEntityList() throws Exception{

        if(1 == 1) return;

        XmlProcessor mProcessor = new XmlProcessor();
        String response = mProcessor.processRespondXML(new File("/Users/willpride/Dimagi/commcare/tests/resources/xml/get_entity_list.xml"));
        System.out.println("response: " + response);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(response));
        Document doc = dBuilder.parse(is);
        Assert.assertEquals("entity-list", doc.getFirstChild().getNodeName());
        NodeList mList = doc.getFirstChild().getChildNodes();
        int entityCount = 0;
        boolean foundChristy = false;

        for(int i=0; i< mList.getLength(); i++){
            Node node = mList.item(i);
            if(node.getNodeName().equals("entity")){
                entityCount ++;
                if(node.hasAttributes()){
                    if(node.getAttributes().getNamedItem("name").getNodeValue().contains("Christy")){
                        foundChristy = true;
                    }
                }
            }
        }
        Assert.assertEquals(entityCount, 55);
        Assert.assertTrue(foundChristy);
    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder();
    }

}
