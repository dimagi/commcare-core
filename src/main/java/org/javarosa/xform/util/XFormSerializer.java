package org.javarosa.xform.util;

import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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

    /**
     * Formats an XML document into a UTF-8 (no BOM) compatible format
     *
     * @return The raw bytes of the utf-8 encoded doc
     * @throws IOException                           If there is an issue transferring
     *                                               the bytes to a byte stream.
     * @throws UnsupportedUnicodeSurrogatesException If the document contains values
     *                                               that are not UTF-8 encoded.
     */
    public static byte[] getUtfBytesFromDocument(Document doc) throws IOException {
        KXmlSerializer serializer = new KXmlSerializer() {
            @Override
            public XmlSerializer text(String text) throws IOException {
                try {
                    return super.text(text);
                } catch (IllegalArgumentException e) {
                    // certain versions of Android have trouble encoding
                    // unicode characters that require "surrogates".
                    throw new UnsupportedUnicodeSurrogatesException(text);
                }
            }
        };
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Writer osw = new OutputStreamWriter(bos, "UTF-8");
        serializer.setOutput(osw);
        doc.write(serializer);
        serializer.flush();
        return bos.toByteArray();
    }

    public static class UnsupportedUnicodeSurrogatesException extends RuntimeException {
        public UnsupportedUnicodeSurrogatesException(String message) {
            super(message);
        }
    }

}
