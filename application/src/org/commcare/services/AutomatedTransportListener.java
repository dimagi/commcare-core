/**
 * 
 */
package org.commcare.services;

import org.javarosa.services.transport.TransportListener;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.TransportService;

/**
 * @author ctsims
 *
 */
public class AutomatedTransportListener implements TransportListener {
	private static final int FAILURE_THRESHOLD = 2;
	
	private int failureCount = 0;
	private boolean engaged = false;
	
	public AutomatedTransportListener() {
		
	}
	
	public boolean engaged() {
		return engaged;
	}
	
	public void reinit() {
		failureCount = 0;
		engaged = true;
	}
	
	public void expire() {
		engaged = false;
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
		if(!(message.isSuccess())) {
			failureCount++;
		}
		if(failureCount > FAILURE_THRESHOLD) {
			TransportService.halt();
			//The listener gets halted explicitly by the sending service, no need to do so here.
		}
	}

}
