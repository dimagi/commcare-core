/**
 *
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Vector;

import org.commcare.suite.model.StackFrameStep;
import org.commcare.suite.model.StackOperation;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 */
public class StackOpParser extends ElementParser<StackOperation> {

    public StackOpParser(KXmlParser parser) {
        super(parser);
    }

    /* (non-Javadoc)
     * @see org.javarosa.xml.ElementParser#parse()
     */
    public StackOperation parse() throws InvalidStructureException, IOException, XmlPullParserException {
        String operation = parser.getName();

        String ifConditional = parser.getAttributeValue(null, "if");
        //XPath check

        try {
            if ("create".equals(operation)) {
                String id = parser.getAttributeValue(null, "id");
                Vector<StackFrameStep> children = getChildren(operation);
                return StackOperation.CreateFrame(id, ifConditional, children);
            } else if ("push".equals(operation)) {
                Vector<StackFrameStep> children = getChildren(operation);
                return StackOperation.PushData(ifConditional, children);
            } else if ("clear".equals(operation)) {
                String id = parser.getAttributeValue(null, "frame");
                if (this.nextTagInBlock("clear")) {
                    //This means there are children of the clear, no good!
                    throw new InvalidStructureException("The <clear> operation does not support children", this.parser);
                }
                return StackOperation.ClearFrame(id, ifConditional);
            } else {
                throw new InvalidStructureException("<" + operation + "> is not a valid stack operation!", this.parser);
            }
        } catch (XPathSyntaxException e) {
            throw new InvalidStructureException("Invalid condition expression for " + operation + " operation: " + ifConditional + ".\n" + e.getMessage(), parser);
        }
    }

    private Vector<StackFrameStep> getChildren(String operation) throws InvalidStructureException, IOException, XmlPullParserException {
        Vector<StackFrameStep> elements = new Vector<StackFrameStep>();
        StackFrameStepParser sfep = new StackFrameStepParser(parser);
        while (nextTagInBlock(operation)) {
            elements.addElement(sfep.parse());
        }
        return elements;
    }
}
