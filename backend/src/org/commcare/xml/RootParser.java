/**
 * 
 */
package org.commcare.xml;

import org.commcare.suite.model.Root;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;

/**
 * @author ctsims
 *
 */
public class RootParser extends ElementParser<Root> {

	public RootParser(KXmlParser parser) {
		super(parser);
	}

	/* (non-Javadoc)
	 * @see org.commcare.xml.ElementParser#parse()
	 */
	public Root parse() throws InvalidStructureException {
		this.checkNode("root");
		
		String id = parser.getAttributeValue(null, "prefix");
		String readonly = parser.getAttributeValue(null, "readonly");
		
		//Get the child or error out if none exists
		if(!(nextTagInBlock("root"))) {
			throw new InvalidStructureException();
		}
		
		String referenceType = parser.getName().toLowerCase();
		if(referenceType.equals("filesystem")) {
			
		} else if(referenceType.equals("resource")) {
			
		} else if(referenceType.equals("absolute")) {
			
		}
		
		return new Root();
	}

}
