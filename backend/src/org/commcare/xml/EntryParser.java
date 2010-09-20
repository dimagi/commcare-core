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
	public Entry parse() throws InvalidStructureException, IOException, XmlPullParserException {
		this.checkNode("entry");
		
		String xFormNamespace = "";
		Hashtable<String, String> references = new Hashtable<String,String>();
		String shortDetailId = null;
		String longDetailId = null;
		String commandId = "";
		Text commandText = null;
		String imageURI = null;
		String audioURI = null;
		Object[] displayArr;  //Should *ALWAYS* be [Text commandText, String imageURI, String audioURI]
			
		while(nextTagInBlock("entry")) {
			if(parser.getName().equals("form")) {
				xFormNamespace = parser.nextText();
//				imageURI = parser.getAttributeValue
			}
			else if(parser.getName().equals("command")) {
				commandId = parser.getAttributeValue(null, "id");
				parser.nextTag();
				if(parser.getName().equals("text")){
					commandText = new TextParser(parser).parse();
				}else if(parser.getName().equals("display")){
					displayArr = parseDisplayBlock();
					//check that we have a commandText;
					if(displayArr[0] == null) throw new InvalidStructureException("Expected CommandText in Display block",parser);
					else commandText = (Text)displayArr[0];
					
					imageURI = (String)displayArr[1];
					audioURI = (String)displayArr[2];
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
		Entry e = new Entry(commandId, commandText, longDetailId, shortDetailId, references, xFormNamespace, imageURI, audioURI);
		return e;
	}
	
	public Object[] parseDisplayBlock() throws InvalidStructureException, IOException, XmlPullParserException{
		Object[] info = new Object[3];
		while(nextTagInBlock("display")){
			if(parser.getName().equals("text")){
				info[0] = new TextParser(parser).parse();
			}
			//check and parse media stuff
			else if(parser.getName().equals("media")){  
				info[1] = parser.getAttributeValue(null, "image");
				info[2] = parser.getAttributeValue(null, "audio");
				//only ends up grabbing the last entries with 
				//each attribute, but we can only use one of each anyway.
			}
		}
		return info;
	}
}
