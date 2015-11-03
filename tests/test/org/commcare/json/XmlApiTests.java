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
public class XmlApiTests {

    MockApp mApp;
    XmlProcessor mProcessor;

    @Before
    public void setUp() throws Exception {
        mProcessor = new XmlProcessor();
    }

    @Test
    public void testEvaluateInstance() throws Exception{
        String response = mProcessor.processRespondXML(new File("/Users/willpride/Dimagi/commcare/tests/resources/xml/test_xpath.xml"));
        Assert.assertEquals(response, "1970-10-23");
    }

    @Test
    public void testGetEntityList() throws Exception{
        String response = mProcessor.processRespondXML(new File("/Users/willpride/Dimagi/commcare/tests/resources/xml/get_entity_list.xml"));
        Document doc = getDocumentFromResponse(response);
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

    @Test
    public void testGetFormInstance() throws Exception{
        String response = mProcessor.processRespondXML(new File("/Users/willpride/Dimagi/commcare/tests/resources/xml/get_form_instance.xml"));
        Document doc = getDocumentFromResponse(response);

        Assert.assertEquals("data", doc.getFirstChild().getNodeName());
        NodeList mList = doc.getFirstChild().getChildNodes();
        for(int i=0; i< mList.getLength(); i++){
            Node node = mList.item(i);
            if(node.getNodeName().equals("q_int")){
                Assert.assertEquals("123", node.getTextContent());
            }
            if(node.getNodeName().equals("q_date")){
                Assert.assertEquals("1970-10-23", node.getTextContent());
            }
        }
    }

    @Test
    public void testGetMenu() throws Exception{
        String response = mProcessor.processRespondXML(new File("/Users/willpride/Dimagi/commcare/tests/resources/xml/get_first_menu.xml"));
        Document doc = getDocumentFromResponse(response);
        Assert.assertEquals("menu", doc.getFirstChild().getNodeName());
        NodeList mList = doc.getFirstChild().getChildNodes();
        int entityCount = 0;
        boolean foundFiltering = false;

        for(int i=0; i< mList.getLength(); i++){
            Node node = mList.item(i);
            if(node.getNodeName().equals("menu-entity")){
                entityCount ++;
                if(node.hasAttributes()){
                    if(node.getAttributes().getNamedItem("name").getNodeValue().equals("Filtering Tests")){
                        foundFiltering = true;
                    }
                }
            }
        }
        Assert.assertEquals(entityCount, 12);
        Assert.assertTrue(foundFiltering);
    }

    @Test
    public void testGetSecondMenu() throws Exception{
        String response = mProcessor.processRespondXML(new File("/Users/willpride/Dimagi/commcare/tests/resources/xml/get_second_menu.xml"));
        Document doc = getDocumentFromResponse(response);

        Assert.assertEquals("menu", doc.getFirstChild().getNodeName());
        NodeList mList = doc.getFirstChild().getChildNodes();
        int entityCount = 0;
        boolean foundCreate = false;

        for(int i=0; i< mList.getLength(); i++){
            Node node = mList.item(i);
            if(node.getNodeName().equals("menu-entity")){
                entityCount ++;
                if(node.hasAttributes()){
                    if(node.getAttributes().getNamedItem("name").getNodeValue().equals("Create a Case")){
                        foundCreate = true;
                    }
                }
            }
        }
        Assert.assertEquals(entityCount, 6);
        Assert.assertTrue(foundCreate);
    }

    @Test
    public void testConstraints() throws Exception{
        String response = mProcessor.processRespondXML(new File("/Users/willpride/Dimagi/commcare/tests/resources/xml/form_constraint.xml"));
        Assert.assertEquals(response, "Assert is true");
    }

    @Test
    public void testQuestionTypes() throws Exception{
        String response = mProcessor.processRespondXML(new File("/Users/willpride/Dimagi/commcare/tests/resources/xml/question_types.xml"));

    }


    public Document getDocumentFromResponse(String response){
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(response));
            return dBuilder.parse(is);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder();
    }

}
