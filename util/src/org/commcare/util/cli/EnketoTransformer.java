package org.commcare.util.cli;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;

/**
 * Created by willpride on 10/8/15.
 */
public class EnketoTransformer {

    File xformFile;
    ApplicationHost applicationHost;

    public EnketoTransformer(File xformFile, ApplicationHost host){
        this.xformFile = xformFile;
        this.applicationHost = host;
    }

    public void buildXmlDoc() throws Exception {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse(xformFile);
        scanDocument(document.getDocumentElement());
        writeDocumentToFile(document);
    }

    public void writeDocumentToFile(Document document) throws Exception{
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result =  new StreamResult(new File("xml_output.xml"));
        transformer.transform(source, result);
    }

    public String getDocumentToString(Document document) throws Exception{
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
        return output;
    }

    public void scanDocument(Node node) throws Exception{
        // do something with the current node instead of System.out
        String nodeName = node.getNodeName();
        System.out.println("node name: " + nodeName);
        if(nodeName.equals("instance")){
            for(int i =0; i< node.getAttributes().getLength(); i++){
                if(node.getAttributes().getNamedItem("src") != null){
                    buildInstance(node, node.getAttributes().getNamedItem("src").getNodeValue());
                    return;
                }
            }
        }
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            org.w3c.dom.Node currentNode = nodeList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                //calls this method for all the children which is Element
                scanDocument(currentNode);
            }
        }
    }

    public void buildInstance(Node node, String path) throws Exception{

        String root = getRootFromPath(path);
        String instanceXml = applicationHost.getInstanceXML(path, root);

        Element newNode =  DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(instanceXml.getBytes()))
                .getDocumentElement();

        Node importedNode = node.getOwnerDocument().importNode(newNode, true);
        node.appendChild(importedNode);
    }

    public static String getRootFromPath(String path){
        if(path.contains("casedb")){
            return "case";
        }
        if(path.contains("session")){
            return "session";
        }
        if(path.contains("ledgerdb")){
            return "ledger";
        }
        return null;
    }
}
