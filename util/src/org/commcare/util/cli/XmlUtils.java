package org.commcare.util.cli;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.ledger.instance.LedgerInstanceTreeElement;
import org.commcare.cases.model.Case;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.model.xform.DataModelSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by wpride1 on 9/18/15.
 */
public class XmlUtils {

    public static String getCaseXML(InstanceInitializationFactory iif){
        byte[] bytes = serializeCaseInstanceFromSandbox(iif);
        return getPrettyXml(bytes);
    }

    public static String getLedgerXML(InstanceInitializationFactory iif){
        byte[] bytes = serializeLedgerInstanceFromSandbox(iif);
        return getPrettyXml(bytes);
    }

    public static String getSessionXML(InstanceInitializationFactory iif){
        byte[] bytes = serializeSessionInstanceFromSandbox(iif);
        return getPrettyXml(bytes);
    }

    public static String getFixtureXML(InstanceInitializationFactory iif, String name){
        byte[] bytes = serializeFixtureInstanceFromSandbox(iif, name);
        return getPrettyXml(bytes);
    }

    public static String getInstanceXML(InstanceInitializationFactory iif, String name, String root){
        byte[] bytes = serializeInstanceFromSandbox(iif, name, root);
        return getPrettyXml(bytes);
    }

    public static byte[] serializeInstance(InstanceInitializationFactory iif, String jrPath, String modelName){
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataModelSerializer s = new DataModelSerializer(bos, iif);
            s.serialize(new ExternalDataInstance(jrPath, modelName), null);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static byte[] serializeInstanceFromSandbox(InstanceInitializationFactory iif, String path, String root) {
        System.out.println("serializing instance: " + path + " root " + root);
        return serializeInstance(iif, path, root);
    }

    private static byte[] serializeSessionInstanceFromSandbox(InstanceInitializationFactory iif) {
        return serializeInstance(iif, "jr://instance/session", "session");
    }

    private static byte[] serializeLedgerInstanceFromSandbox(InstanceInitializationFactory iif) {
        return serializeInstance(iif, "jr://instance/ledgerdb", LedgerInstanceTreeElement.MODEL_NAME);
    }

    public static byte[] serializeCaseInstanceFromSandbox(InstanceInitializationFactory iif) {
        return serializeInstance(iif, "jr://instance/casedb", CaseInstanceTreeElement.MODEL_NAME);
    }


    public static byte[] serializeFixtureInstanceFromSandbox(InstanceInitializationFactory iif, String name) {
        return serializeInstance(iif, "jr://instance/fixture/" + name, null);
    }

    //fromXML util
    public static String getPrettyXml(byte[] xml) {
        try {
            String unformattedXml = new String(xml);
            final Document document = parseXmlFile(unformattedXml);

            OutputFormat format = new OutputFormat(document);
            format.setLineWidth(65);
            format.setIndenting(true);
            format.setIndent(2);
            Writer out = new StringWriter();
            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.serialize(document);

            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Document parseXmlFile(String in) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(in));
            return db.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
