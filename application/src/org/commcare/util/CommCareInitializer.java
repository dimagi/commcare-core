/**
 * 
 */
package org.commcare.util;

import org.commcare.view.CommCareStartupInteraction;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.j2me.log.HandledThread;
import org.javarosa.log.activity.DeviceReportState;
import org.javarosa.log.properties.LogPropertyRules;
import org.javarosa.log.util.LogReportUtils;


/**
 * @author ctsims
 *
 */
public abstract class CommCareInitializer implements Runnable {

	protected static final int RESPONSE_NONE = 0;
	protected static final int RESPONSE_YES = 1;
	protected static final int RESPONSE_NO = 2;
	
	private InitializationListener listener;
	int response = RESPONSE_NONE;

	public void initialize(InitializationListener listener) {
		this.listener = listener;
		HandledThread t = new HandledThread(this);
		listener.setInitThread(t);
		t.start();
	}
	
	protected abstract boolean runWrapper() throws UnfullfilledRequirementsException;
	
	public void run() {
		try {
			if(runWrapper()) {
				listener.onSuccess();
			} else {
				listener.onFailure();
			}
		} catch(Exception e) {
			Logger.exception(e);
			fail(e);
		}
	}
	
	protected void fail(Exception e) {
		if(blockForResponse(CommCareStartupInteraction.failSafeText("commcare.fail", "There was an error, and CommCare could not be started. Do you want to see the debug information?"))) {
			if(blockForResponse(e.getMessage() + "\n" + CommCareStartupInteraction.failSafeText("commcare.fail.sendlogs", "Attempt to send logs?"), true)) {
				setMessage("Sending...");
				DeviceReportState logSubmit = new DeviceReportState(LogReportUtils.REPORT_FORMAT_FULL) {
				
					public String getDestURL() {
						String url = PropertyManager._().getSingularProperty(LogPropertyRules.LOG_SUBMIT_URL);
						if(url == null) {
							url = CommCareContext._().getSubmitURL();
						}
						return url;
					}

					public void done() {
						// TODO Auto-generated method stub
						blockForResponse("Attempt made", false);
						listener.onFailure();
					}
				};
				logSubmit.addSubReport(new ResourceTableSubreport(CommCareContext.RetrieveGlobalResourceTable()));
				logSubmit.start();
			}
		}
		listener.onFailure();
	}
	
	protected abstract void setMessage(String message);
	
	protected abstract void askForResponse(String message, YesNoListener listener, boolean yesNo);
	
	protected boolean blockForResponse(String message) {
		return this.blockForResponse(message, true);
	}
	
	protected boolean blockForResponse(String message, boolean yesNo) {
		response = RESPONSE_NONE;
		askForResponse(message,  new YesNoListener() {
			public void no() {
				CommCareInitializer.this.response = CommCareInitializer.RESPONSE_NO;
			}
			public void yes() {
				CommCareInitializer.this.response = CommCareInitializer.RESPONSE_YES;
			}
			
		}, yesNo);
		while(response == RESPONSE_NONE);
		return response == RESPONSE_YES;
	}
}
