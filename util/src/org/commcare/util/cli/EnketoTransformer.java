package org.commcare.util.cli;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Created by willpride on 10/8/15.
 */
public class EnketoTransformer {

    File xformFile;
    ApplicationHost applicationHost;
    HashMap<String, String> referenceReplacements;

    public EnketoTransformer(File xformFile, ApplicationHost host){
        this.xformFile = xformFile;
        this.applicationHost = host;
        this.referenceReplacements = new HashMap<>();
    }

    public void buildXmlDoc() throws Exception {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse(xformFile);
        scanDocumentForInstances(document.getDocumentElement());
        scanDocumentForReferences(document.getDocumentElement());
        scanDocumentReplaceSetValues(document.getDocumentElement());
        writeDocumentToFile(document);
    }

    private void scanDocumentReplaceSetValues(Node node) throws Exception{
        if(node.getNodeName().contains("setvalue") &&
                node.getAttributes().getNamedItem("event").getNodeValue().equals("xforms-ready")){
            System.out.println("fire");
            fireReference(node);
        }
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                scanDocumentReplaceSetValues(currentNode);
            }
        }
    }

    private void fireReference(Node node) throws Exception{
        Document document = node.getOwnerDocument();
        String ref = node.getAttributes().getNamedItem("ref").getNodeValue();
        Node value = node.getAttributes().getNamedItem("value");
        XPath xPath = XPathFactory.newInstance().newXPath();
        String fullRef = "/html/head/model/instance" + ref;
        System.out.println(" ref " + fullRef + " value " + value);
        NodeList nodes = (NodeList)xPath.evaluate(fullRef,
                document.getDocumentElement(), XPathConstants.NODESET);
        if(nodes.getLength() > 1 || nodes.getLength() == 0) {
            throw new Exception ("Returned " + nodes.getLength() + " nodes with ref: " + fullRef);
        }
        nodes.item(0).setNodeValue(value.getNodeValue());
    }

    private void scanDocumentForReferences(Node node) throws Exception {
        // do something with the current node instead of System.out
        String nodeName = node.getNodeName();
        if(nodeName.equals("bind")){
            replaceReference(node, "calculate");
        } else if(nodeName.equals("setvalue")){
            replaceReference(node, "value");
        }
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            org.w3c.dom.Node currentNode = nodeList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                scanDocumentForReferences(currentNode);
            }
        }
    }

    private void replaceReference(Node node, String attributeName) {
        Node attribute = node.getAttributes().getNamedItem(attributeName);
        if(attribute == null){return;}
        String calculate = attribute.getNodeValue();
        for(String key: referenceReplacements.keySet()){
            if(calculate.contains(key)){
                String recalculate = calculate.replace(key, referenceReplacements.get(key));
                attribute.setNodeValue(recalculate);
            }
        }
    }

    public void writeDocumentToFile(Document document) throws Exception{
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result =  new StreamResult(new File("xml_output.xml"));
        transformer.transform(source, result);
    }

    public void scanDocumentForInstances(Node node) throws Exception{
        // do something with the current node instead of System.out
        String nodeName = node.getNodeName();
        if(nodeName.equals("instance")){
            for(int i =0; i< node.getAttributes().getLength(); i++){
                if(node.getAttributes().getNamedItem("src") != null){
                    buildInstance(node, node.getAttributes().getNamedItem("src").getNodeValue());
                    addReplaceReference(node);
                    return;
                }
            }
        }
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            org.w3c.dom.Node currentNode = nodeList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                scanDocumentForInstances(currentNode);
            }
        }
    }

    private void addReplaceReference(Node node) {
        String id = node.getAttributes().getNamedItem("id").getNodeValue();
        String originalReference = "instance('" + id + "')";
        String newReference = "/instance[@id='" + id + "']";
        referenceReplacements.put(originalReference, newReference);

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
