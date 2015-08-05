/**
 *
 */
package org.commcare.util;

import org.commcare.data.xml.DataModelPullParser;
import org.commcare.xml.CommCareElementParser;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * This class contains the process for handling an XML response from an OpenRosa
 * server. As time goes on, this class should dispatch different API versions to
 * different handlers or sections of code so that there is a shared long-term source
 * of capacity for handling server responses.
 *
 * @author ctsims
 *
 */
public class OpenRosaApiResponseProcessor {

    public static final String ONE_OH = "1.0";

    CommCareTransactionParserFactory factory = new CommCareTransactionParserFactory(true);

    /**
     * Whether the processor knows that it is capable of processing the response to
     * the current message.
     *
     * @param message A completed transport message with an available response.
     *
     * @return true if the processor should be able to handle the response. false otherwise.
     */
    public boolean handlesResponse(SimpleHttpTransportMessage message) {
        //The != null should be unnecessary going forward, but just being careful since it might not
        //get cached properly?
        //TODO: Better way of saying "this is the list of know API versions"
        if(message.getResponseProperties() != null && ONE_OH.equals(message.getResponseProperties().getORApiVersion())) {
            return true;
        }
        return false;
    }

    /**
     * Process the response to the current message (may have side effects).
     *
     * @param message A completed transport message with an available response.
     * @return A string with the user facing messages which were parsed out from that response.
     * @throws InvalidStructureException If the response is present, but incorrectly structured
     * @throws IOException
     * @throws UnfullfilledRequirementsException If the isn't capable of processing the provided message
     * for well recognized reasons (Like the API version of the response being above that currently understood)
     * @throws XmlPullParserException
     */
    public String processResponse(SimpleHttpTransportMessage message) throws InvalidStructureException, IOException, UnfullfilledRequirementsException, XmlPullParserException {

        if(message.getResponseProperties() != null && ONE_OH.equals(message.getResponseProperties().getORApiVersion())) {

            //TODO: Eliminate byte arrays, and replace with an active stream of the response
            byte[] response = message.getResponseBody();

            DataModelPullParser parser = new DataModelPullParser(new ByteArrayInputStream(response), factory);

            boolean success = parser.parse();

            if(factory.getResponseMessage() != null) {
                return factory.getResponseMessage();
            } else {
                return null;
            }
        }
        //throw some exception
        throw new UnfullfilledRequirementsException("Unrecognized response type", CommCareElementParser.SEVERITY_ENVIRONMENT);
    }

    public String[] getCompiledResponses() {
        OrderedHashtable<String,String> messageMap = factory.getResponseMessageMap();
        String[] response = new String[messageMap.size()];
        for(int i = 0 ; i < messageMap.size(); ++i ) {
            response[i] = (String)messageMap.elementAt(i);
        }
        return response;
    }
}
