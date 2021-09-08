package org.commcare.util;

import org.javarosa.engine.xml.XmlUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author $|-|!˅@M
 */
public class XmlUtilTest {

    @Test
    public void testPrettifyXml() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/minified_xml.xml")) {
            byte[] bytes = inputStream.readAllBytes();
            String actualOutput = XmlUtil.getPrettyXml(bytes);

            String expectedOutput = getPrettyXml();
            Assert.assertEquals(expectedOutput, actualOutput);
        }
    }

    public String getPrettyXml() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/pretty_printed_xml.xml")) {
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            for (int result = bis.read(); result != -1; result = bis.read()) {
                buf.write((byte) result);
            }
            return buf.toString("UTF-8");
        }
    }
}
