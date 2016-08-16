package org.commcare.xml;

import org.commcare.suite.model.StackFrameStep;
import org.commcare.suite.model.StackOperation;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Vector;

/**
 * @author ctsims
 */
public class StackOpParser extends ElementParser<StackOperation> {

    public StackOpParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public StackOperation parse() throws InvalidStructureException, IOException, XmlPullParserException {
        String operation = parser.getName();

        String ifConditional = parser.getAttributeValue(null, "if");

        try {
            switch (operation) {
                case "create":
                    String id = parser.getAttributeValue(null, "id");
                    return StackOperation.buildCreateFrame(id, ifConditional, getChildren(operation));
                case "push":
                    return StackOperation.buildPushFrame(ifConditional, getChildren(operation));
                case "clear":
                    String clearId = parser.getAttributeValue(null, "frame");
                    if (nextTagInBlock("clear")) {
                        //This means there are children of the clear, no good!
                        throw new InvalidStructureException("The <clear> operation does not support children", this.parser);
                    }
                    return StackOperation.buildClearFrame(clearId, ifConditional);
                default:
                    throw new InvalidStructureException("<" + operation + "> is not a valid stack operation!", this.parser);
            }
        } catch (XPathSyntaxException e) {
            throw new InvalidStructureException("Invalid condition expression for " + operation + " operation: " + ifConditional + ".\n" + e.getMessage(), parser);
        }
    }

    private Vector<StackFrameStep> getChildren(String operation) throws InvalidStructureException, IOException, XmlPullParserException {
        Vector<StackFrameStep> elements = new Vector<>();
        StackFrameStepParser sfep = new StackFrameStepParser(parser);
        while (nextTagInBlock(operation)) {
            elements.addElement(sfep.parse());
        }
        return elements;
    }
}
