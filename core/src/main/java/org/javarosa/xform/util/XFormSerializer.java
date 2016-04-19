package org.javarosa.xform.util;

import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class XFormSerializer {

    public static String elementToString(Element e) {
        KXmlSerializer serializer = new KXmlSerializer();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        String s;
        try {
            serializer.setOutput(dos, null);
            e.write(serializer);
            serializer.flush();
            s = new String(bos.toByteArray(), "UTF-8");
            return s;
        } catch (UnsupportedEncodingException uce) {
            uce.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return null;
    }

    public static byte[] getUtfBytes(Document doc) {
        KXmlSerializer serializer = new KXmlSerializer();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            Writer osw = new OutputStreamWriter(bos, "UTF-8");
            serializer.setOutput(osw);
            doc.write(serializer);
            serializer.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
