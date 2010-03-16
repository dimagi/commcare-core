/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;

import org.commcare.suite.model.Filter;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class FilterParser extends ElementParser<Filter> {

	public FilterParser(KXmlParser parser) {
		super(parser);
	}
	
	/* (non-Javadoc)
	 * @see org.commcare.xml.ElementParser#parse()
	 */
	public Filter parse() throws InvalidStructureException, IOException, XmlPullParserException {
		checkNode("filter");
		
		getNextTagInBlock("filter");
		
		//We should now be on the actual filter.
		String handler = parser.getName().toLowerCase();
		
		if(handler.equals("raw")) {
			String function =  parser.getAttributeValue(null,"function");
			return new Filter();
		} else if(handler.equals("referral")) {
			String function =  parser.getAttributeValue(null,"function");
			String caseType = parser.getAttributeValue(null,"case-type");
			String referralType = parser.getAttributeValue(null,"referral-type");
			return new Filter();
		} else if(handler.equals("case")) {
			String function =  parser.getAttributeValue(null,"function");
			String caseType = parser.getAttributeValue(null,"case-type");
			return new Filter();
		} else{
			throw new InvalidStructureException("Unrecognized filter type " + handler,parser);
		}
	}

}
