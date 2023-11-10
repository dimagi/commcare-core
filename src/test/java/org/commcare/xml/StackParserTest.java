package org.commcare.xml;

import org.commcare.session.SessionFrame;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.suite.model.StackOperation;
import org.javarosa.xml.util.InvalidStructureException;
import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class StackParserTest {

    @Test
    public void testParseRewind() throws IOException, InvalidStructureException, XmlPullParserException {
        String xml = "<push><rewind /></push>";
        StackOpParser parser = ParserTestUtils.buildParser(xml, StackOpParser::new);
        StackOperation op = parser.parse();
        Assert.assertEquals(StackOperation.OPERATION_PUSH, op.getOp());
        StackFrameStep steps = op.getStackFrameSteps().get(0);
        Assert.assertEquals(steps.getElementType(), SessionFrame.STATE_REWIND);
        System.out.println(steps.getValue());
        Assert.assertNull(steps.getValue());
    }
}
