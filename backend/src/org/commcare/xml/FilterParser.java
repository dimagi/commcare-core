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
		
		Filter f = Filter.EmptyFilter();
		
		//getNextTagInBlock("filter");
		while(nextTagInBlock("filter")) {
			
			//We should now be on the actual filter.
			String handler = parser.getName().toLowerCase();
			
			if(handler.equals("raw")) {
				String function =  parser.getAttributeValue(null,"function");
				f = f.merge(Filter.RawFilter(function));
			} else if(handler.equals("referral")) {
				String referralType = parser.getAttributeValue(null,"referral-type");
				String resolved = parser.getAttributeValue(null,"view-resolved");
				f = f.merge(Filter.ReferralFilter(referralType, new Boolean(true).toString().equals(resolved)));
			} else if(handler.equals("case")) {
				String seeClosed = parser.getAttributeValue(null,"view-closed");
				String caseType = parser.getAttributeValue(null,"case-type");
				f = f.merge(Filter.CaseFilter(caseType, new Boolean(true).toString().equals(seeClosed)));
			} else{
				throw new InvalidStructureException("Unrecognized filter type " + handler,parser);
			}
		}
		return f;
	}

}
