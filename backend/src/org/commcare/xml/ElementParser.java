/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.io.InputStream;

import org.commcare.resources.model.Resource;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public abstract class ElementParser<T> {
	KXmlParser parser;
	
	T element;
	
	int level = 0;
	
	public ElementParser(Resource resource) {
		this(resource.OpenStream());
	}
	
	public ElementParser(InputStream suiteStream) {
		parser = new KXmlParser();
		try {
			parser.setInput(suiteStream,"UTF-8");
			//parser.setFeature(KXmlParser., arg1)
			parser.next();
			
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ElementParser(KXmlParser parser) {
		this.parser = parser;
		level = parser.getDepth();
	}
	
	protected void checkNode(String name) throws InvalidStructureException {
		if(!parser.getName().toLowerCase().equals(name)) {
			throw new InvalidStructureException();
		}
	}
	
	protected boolean nextTagInBlock(String terminal) throws InvalidStructureException {
        int eventType;
		try {
			eventType = parser.nextTag();
			
            if(eventType == KXmlParser.START_DOCUMENT) {
                //
            } else if(eventType == KXmlParser.END_DOCUMENT) {
                return false;
            } else if(eventType == KXmlParser.START_TAG) {
                return true;
            } else if(eventType == KXmlParser.END_TAG) {
                //If we've reached the end of the current node path, 
                //return false (signaling that the parsing action should end).
                if(parser.getName().toLowerCase().equals(terminal)) { return false; }
                //Elsewise, as long as we haven't left the current context, keep diving
                else if(parser.getDepth() >= level) { return nextTagInBlock(terminal); }
                //if we're below the limit, get out.
                else { return false; }
            } else if(eventType == KXmlParser.TEXT) {
                return true;
            }
            return true;
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			throw new InvalidStructureException();
		} catch (IOException e) {
			throw new InvalidStructureException();
		}
	}
	
	protected boolean nextTagInBlock() throws InvalidStructureException {
		return nextTagInBlock(null);
	}
	
	protected int parseInt(String value) throws InvalidStructureException  {
		if(value == null) {
			throw new InvalidStructureException();
		}
		try  {
			return Integer.parseInt(value);
		} catch(NumberFormatException nfe) {
			throw new InvalidStructureException();
		}
	}
	
	public abstract T parse() throws InvalidStructureException;
}
