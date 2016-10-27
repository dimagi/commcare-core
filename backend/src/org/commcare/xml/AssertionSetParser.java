/**
 *
 */
package org.commcare.xml;

import org.commcare.suite.model.AssertionSet;
import org.commcare.suite.model.Text;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Vector;

/**
 * @author ctsims
 */
public class AssertionSetParser extends ElementParser<AssertionSet> {

    public AssertionSetParser(KXmlParser parser) {
        super(parser);
    }

    /* (non-Javadoc)
     * @see org.javarosa.xml.ElementParser#parse()
     */
    @Override
    public AssertionSet parse() throws InvalidStructureException, IOException, XmlPullParserException {
        this.checkNode("assertions");

        Vector<String> tests = new Vector<>();
        Vector<Text> messages = new Vector<>();


        while (nextTagInBlock("assertions")) {
            if (parser.getName().equals("assert")) {
                String test = parser.getAttributeValue(null, "test");
                if (test == null) {
                    throw new InvalidStructureException("<assert> element must have a test attribute!", parser);
                }
                try {
                    XPathParseTool.parseXPath(test);
                } catch (XPathSyntaxException e) {
                    throw new InvalidStructureException("Invalid assertion test : " + test + "\n" + e.getMessage(), parser);
                }
                parser.nextTag();
                checkNode("text");
                Text message = new TextParser(parser).parse();
                tests.addElement(test);
                messages.addElement(message);

            } else {
                throw new InvalidStructureException("Unknown test : " + parser.getName(), parser);
            }
        }
        return new AssertionSet(tests, messages);
    }
}
