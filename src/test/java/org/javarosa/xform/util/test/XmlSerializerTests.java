package org.javarosa.xform.util.test;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.utils.FormLoadingUtils;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.core.services.transport.payload.IDataPayload;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xml.util.InvalidStructureException;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author $|-|!˅@M
 */
public class XmlSerializerTests {

    private static final String formPath = "/test_nonbmpchar.xml";

    @Test
    public void testParseXmlWithNonBMPCharacters() {
        FormInstance model = null;
        try {
            model = FormLoadingUtils.loadFormInstance(formPath);
            // Serialize the xml containing special characters.
            IDataPayload payload = new XFormSerializingVisitor().createSerializedPayload(model);
            assertTrue(payload instanceof ByteArrayPayload);
            assertTrue(payload.getPayloadType() == IDataPayload.PAYLOAD_TYPE_XML);
            ByteArrayInputStream is = (ByteArrayInputStream)payload.getPayloadStream();
            try {
                // Check if we are able to parse the serialized xml.
                // If all the special characters are converted to their corresponding codepoint this would pass.
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                dBuilder.parse(is);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        } catch (IOException e) {
            fail("Unable to load form at " + formPath);
        } catch (InvalidStructureException e) {
            fail("Form at " + formPath + " has an invalid structure.");
        }
    }
}
