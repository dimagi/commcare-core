package org.javarosa.core.model.test;

import org.javarosa.core.model.QuestionDataExtension;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.UploadQuestionExtension;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.xform.parse.QuestionExtensionParser;
import org.javarosa.xform.parse.UploadQuestionExtensionParser;
import org.javarosa.xform.parse.XFormParseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for UploadQuestionExtensionParser and UploadQuestionExtension
 */
public class UploadExtensionTest {

    private Vector<QuestionExtensionParser> extensionParsers;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void init() {
        extensionParsers = new Vector<>();
        extensionParsers.add(new UploadQuestionExtensionParser());
    }

    @Test
    public void testParseMaxDimenWithPx() {
        FormParseInit formWithPx = new FormParseInit("/xform_tests/test_upload_extension_1.xml", extensionParsers);
        QuestionDef q = formWithPx.getFirstQuestionDef();

        Vector<QuestionDataExtension> extensions = q.getExtensions();
        assertEquals("There should be exactly one QuestionDataExtension registered with this QuestionDef",
                1, extensions.size());

        QuestionDataExtension ext = extensions.get(0);
        assertTrue("The extension registered was not an UploadQuestionExtension", ext instanceof UploadQuestionExtension);

        int maxDimen = ((UploadQuestionExtension)ext).getMaxDimen();
        assertEquals("Parsed value of max dimen was incorrect", 800, maxDimen);
    }

    @Test
    public void testParseMaxDimenWithoutPx() {
        FormParseInit formWithoutPx = new FormParseInit("/xform_tests/test_upload_extension_2.xml", extensionParsers);
        QuestionDef q = formWithoutPx.getFirstQuestionDef();

        Vector<QuestionDataExtension> extensions = q.getExtensions();
        assertEquals("There should be exactly one QuestionDataExtension registered with this QuestionDef",
                1, extensions.size());

        QuestionDataExtension ext = extensions.get(0);
        assertTrue("The extension registered was not an UploadQuestionExtension", ext instanceof UploadQuestionExtension);

        int maxDimen = ((UploadQuestionExtension)ext).getMaxDimen();
        assertEquals("Parsed value of max dimen was incorrect", 800, maxDimen);
    }

    @Test
    public void testParseInvalidMaxDimen() {
        exception.expect(XFormParseException.class);
        exception.expectMessage("Invalid input for image max dimension: bad_dimen");
        new FormParseInit("/xform_tests/test_upload_extension_3.xml", extensionParsers);
    }

}
