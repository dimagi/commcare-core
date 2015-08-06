/**
 *
 */
package org.commcare.services;

import org.commcare.util.CommCareHQResponder;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
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
    private int successCount = 0;
    private boolean engaged = false;
    CommCareHQResponder responder;
    TransportListener repeater;

    public AutomatedTransportListener() {
        responder = new CommCareHQResponder(PropertyManager._().getSingularProperty(JavaRosaPropertyRules.OPENROSA_API_LEVEL));
    }

    public boolean engaged() {
        return engaged;
    }

    public void reinit() {
        synchronized(this) {
            failureCount = 0;
            successCount = 0;
            engaged = true;
        }
    }

    public void expire() {
        engaged = false;
        repeater = null;
    }


    /* (non-Javadoc)
     * @see org.javarosa.services.transport.TransportListener#onChange(org.javarosa.services.transport.TransportMessage, java.lang.String)
     */
    public void onChange(TransportMessage message, String remark) {
        synchronized(this) {
            //Irrelevant other than to dispatch
            if(repeater != null) { repeater.onChange(message, remark); }
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.services.transport.TransportListener#onStatusChange(org.javarosa.services.transport.TransportMessage)
     */
    public void onStatusChange(TransportMessage message) {
        synchronized(this) {
            if(repeater != null) { repeater.onStatusChange(message);}
            if(!(message.isSuccess())) {
                failureCount++;
            } else {
                successCount++;

                //Process the response for any relevant information
                responder.getResponseMessage(message);

                //TODO: Log?
            }
            if(failureCount > FAILURE_THRESHOLD) {
                TransportService.halt();
                //The listener gets halted explicitly by the sending service, no need to do so here.
            }
        }
    }

    /**
     * Note: Only valid after not engaged.
     *
     * @return True if the sender has failed to send successfully, false
     * otherwise.
     */
    public boolean failed() {
        return failureCount > FAILURE_THRESHOLD || successCount ==0;
    }

    public void attachRepeater(TransportListener repeater) {
        synchronized(this) {
            this.repeater = repeater;
        }
    }

}
