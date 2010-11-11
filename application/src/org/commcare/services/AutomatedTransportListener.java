/**
 * 
 */
package org.commcare.services;

import org.javarosa.services.transport.TransportListener;
import org.javarosa.services.transport.TransportMessage;

/**
 * @author ctsims
 *
 */
public class AutomatedTransportListener implements TransportListener {
	int failureCount = 0;
	
	public AutomatedTransportListener() {
		
	}

	/* (non-Javadoc)
	 * @see org.javarosa.services.transport.TransportListener#onChange(org.javarosa.services.transport.TransportMessage, java.lang.String)
	 */
	public void onChange(TransportMessage message, String remark) {
		//Irrelevant
	}

	/* (non-Javadoc)
	 * @see org.javarosa.services.transport.TransportListener#onStatusChange(org.javarosa.services.transport.TransportMessage)
	 */
	public void onStatusChange(TransportMessage message) {

	}

}
