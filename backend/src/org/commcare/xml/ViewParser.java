/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Hashtable;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
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
		Hashtable<String, String> references = new Hashtable<String,String>();
		String shortDetailId = null;
		String longDetailId = null;
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
			else if(parser.getName().equals("entity")) {
				this.nextTagInBlock("type");
				String type = parser.nextText();
				
				String reference = type;
				if(this.nextTagInBlock("entity")) {
					reference = parser.nextText();
				}
				references.put(reference,type);
			}
			else if(parser.getName().equals("details")) {
				if(this.nextTagInBlock("details")) {
					//short
					if(parser.getName().toLowerCase().equals("short")) {
						shortDetailId = parser.getAttributeValue(null,"id");
					}
					else {
						throw new InvalidStructureException("Expected at least one short as the first element of detail, found " + parser.getName(),parser);
					}
				}
				if(this.nextTagInBlock("details")) {
					//long
					if(parser.getName().toLowerCase().equals("long")) {
						longDetailId = parser.getAttributeValue(null,"id");
					} else {
						throw new InvalidStructureException("Expected only a long as the second element of a detail, found " + parser.getName(), parser);
					}
				}
			}
		}
		Entry e = new Entry(commandId, commandText, longDetailId, shortDetailId, references, xFormNamespace);
		return e;
	}
}