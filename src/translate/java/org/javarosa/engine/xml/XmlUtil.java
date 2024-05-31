package org.javarosa.engine.xml;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Basic static methods for manipulating raw XML
 *
 * @author ctsims
 */
public class XmlUtil {

    public static String getPrettyXml(byte[] xml) {
        try {
            String unformattedXml = new String(xml);
            final Document document = parseXmlFile(unformattedXml);
            StringWriter stringWriter = new StringWriter();

            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impls = (DOMImplementationLS)registry.getDOMImplementation("LS");

            LSOutput lsOutput = impls.createLSOutput();
            lsOutput.setEncoding("UTF-8");
            lsOutput.setCharacterStream(stringWriter);

            LSSerializer lsSerializer = impls.createLSSerializer();
            DOMConfiguration domConfig = lsSerializer.getDomConfig();
            domConfig.setParameter("format-pretty-print", true);
            domConfig.setParameter("cdata-sections", true);

            // LSSerializer does not have an explicit control for ending the XML Declaration with a
            // newline, hence the need for the property isStandalone
            // More in https://bugs.openjdk.org/browse/JDK-8259502
            domConfig.setParameter("http://www.oracle.com/xml/jaxp/properties/isStandalone", true);

            lsSerializer.setNewLine(System.lineSeparator());
            lsSerializer.write(document, lsOutput);
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Document parseXmlFile(String in) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(in));
            return db.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
