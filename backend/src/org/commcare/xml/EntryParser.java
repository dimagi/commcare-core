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
 * @author ctsims
 *
 */
public class EntryParser extends ElementParser<Entry> {

	public EntryParser(KXmlParser parser) {
		super(parser);
	}

	/* (non-Javadoc)
	 * @see org.commcare.xml.ElementParser#parse()
	 */
	public Entry parse() throws InvalidStructureException {
		if(!parser.getName().toLowerCase().equals("entry")) {
			throw new InvalidStructureException();
		}
		
		try {
		
		String xFormNamespace = "";
		Hashtable<String, String> references = new Hashtable<String,String>();
		String shortDetailId = null;
		String longDetailId = null;
		String commandId = "";
		Text commandText = null;
			
		while(nextTagInBlock("entry")) {
			if(parser.getName().equals("form")) {
				xFormNamespace = parser.nextText();
			}
			else if(parser.getName().equals("command")) {
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
						throw new InvalidStructureException();
					}
				}
				if(this.nextTagInBlock("details")) {
					//long
					if(parser.getName().toLowerCase().equals("long")) {
						longDetailId = parser.getAttributeValue(null,"id");
					} else {
						throw new InvalidStructureException();
					}
				}
			}
		}
		Entry e = new Entry(commandId, commandText, longDetailId, shortDetailId, references, xFormNamespace);
		return e;
		
		} catch(IOException e) {
			e.printStackTrace();
			throw new InvalidStructureException();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			throw new InvalidStructureException();
		}
	}
}
