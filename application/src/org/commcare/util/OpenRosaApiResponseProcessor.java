/**
 * 
 */
package org.commcare.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.commcare.data.xml.DataModelPullParser;
import org.commcare.xml.util.InvalidStructureException;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class OpenRosaApiResponseProcessor {
	
	public static final String ONE_OH = "1.0"; 
	
	public boolean handlesResponse(SimpleHttpTransportMessage message) {
		//The != null should be unnecessary going forward, but just being careful since it might not 
		//get cached properly?
		//TODO: Better way of saying "this is the list of know API versions"
		if(message.getResponseProperties() != null && message.getResponseProperties().getORApiVersion().equals(ONE_OH)) {
			return true;
		}
		return false;
	}
	
	public String processResponse(SimpleHttpTransportMessage message) throws InvalidStructureException, IOException, UnfullfilledRequirementsException, XmlPullParserException {
		if(message.getResponseProperties() != null && message.getResponseProperties().getORApiVersion().equals(ONE_OH)) {
			byte[] response = message.getResponseBody();    			
			CommCareTransactionParserFactory factory = new CommCareTransactionParserFactory(true);
			DataModelPullParser parser = new DataModelPullParser(new ByteArrayInputStream(response), factory);
			
			boolean success = parser.parse();
			
			if(factory.getResponseMessage() != null) {
				return factory.getResponseMessage();
			} else {
				return null;
			}
		}
		//throw some exception
		throw new UnfullfilledRequirementsException("Unrecognized response type", UnfullfilledRequirementsException.SEVERITY_ENVIRONMENT);
	}
}
