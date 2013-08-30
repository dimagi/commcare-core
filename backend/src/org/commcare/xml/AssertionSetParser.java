/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.AssertionSet;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackOperation;
import org.commcare.suite.model.Text;
import org.commcare.xml.util.InvalidStructureException;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class AssertionSetParser extends ElementParser<AssertionSet> {

	public AssertionSetParser(KXmlParser parser) {
		super(parser);
	}
	
	/* (non-Javadoc)
	 * @see org.commcare.xml.ElementParser#parse()
	 */
	public AssertionSet parse() throws InvalidStructureException, IOException, XmlPullParserException {
		this.checkNode("assertions");
		
		Vector<String> tests = new Vector<String>();
		Vector<Text> messages = new Vector<Text>();

		
		while(nextTagInBlock("assertions")) {
			if(parser.getName().equals("assert")) {
				String test = parser.getAttributeValue(null, "test");
				if(test == null) { throw new InvalidStructureException("<assert> element must have a test attribute!", parser); } 
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
