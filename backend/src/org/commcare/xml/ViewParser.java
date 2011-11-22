/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.Text;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * <p>ViewParser parsers the &lt;view&gt; XML structure
 * defining a view object</p> 
 * 
 * @author ctsims
 *
 */
public class ViewParser extends ElementParser<Entry> {

	/**
	 * Creates a new parser for the &lt;view&gt; XML structure
	 * @param parser The xml pull parser being used.
	 */
	public ViewParser(KXmlParser parser) {
		super(parser);
	}

	/* (non-Javadoc)
	 * @see org.commcare.xml.ElementParser#parse()
	 */
	public Entry parse() throws InvalidStructureException, IOException, XmlPullParserException {
		checkNode("view");
		
		String xFormNamespace = null;
		Vector<SessionDatum> data = new Vector<SessionDatum>();
		String commandId = "";
		Text commandText = null;
			
		while(nextTagInBlock("view")) {
			if(parser.getName().equals("command")) {
				commandId = parser.getAttributeValue(null, "id");
				//only child should be a text node.
				if(this.nextTagInBlock("text")) {
					commandText = new TextParser(parser).parse();
					
				}
			}
			else if(parser.getName().equals("session")) {
				this.nextTagInBlock();
				while(parser.getName().equals("datum")) {
					SessionDatumParser parser = new SessionDatumParser(this.parser);
					data.addElement(parser.parse());
				}
			}
		}
		Entry e = new Entry(commandId, commandText, data, xFormNamespace, null, null);
		return e;
	}
}