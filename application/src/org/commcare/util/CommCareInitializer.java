/**
 *
 */
package org.commcare.util;

import org.commcare.view.CommCareStartupInteraction;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.j2me.log.HandledThread;
import org.javarosa.log.activity.DeviceReportState;
import org.javarosa.log.properties.LogPropertyRules;
import org.javarosa.log.util.LogReportUtils;
import org.javarosa.xml.util.UnfullfilledRequirementsException;


/**
 * @author ctsims
 *
 */
public abstract class CommCareInitializer implements Runnable {

    protected static final int RESPONSE_NONE = 0;
    protected static final int RESPONSE_YES = 1;
    protected static final int RESPONSE_NO = 2;

    private String currentOOMMessage;

    public static final String LOG_INIT = "initialization";

    private InitializationListener listener;
    int response = RESPONSE_NONE;

    public CommCareInitializer() {
        currentOOMMessage = CommCareStartupInteraction.failSafeText("commcare.startup.oom","CommCare needs to restart in order to continue installing your application. Please press 'OK' and start CommCare again.");
    }


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
        } catch(OutOfMemoryError e){
            Logger.log(LOG_INIT, "OOM during startup");
            promptRestart();
        }
    }

    /**
     * Signal to the initalizer that progress has been made in advancing the state of the application during
     * an installation. This helps differentiate between OOM errors which occur in a larger install and OOM's
     * which are endemic
     */
    protected void setCurrentOOMMessage(String message){
        this.currentOOMMessage = message;
    }

    private void promptRestart() {
        blockForResponse(currentOOMMessage, false,  null, null);

        //I don't remember, do we have a cleaner way to exit?
        CommCareContext._().getMidlet().notifyDestroyed();
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

    protected abstract void askForResponse(String message, YesNoListener listener, boolean yesNo, String left, String right);

    protected boolean blockForResponse(String message) {
        return this.blockForResponse(message, true);
    }

    protected boolean blockForResponse(String message, String left, String right){
        return this.blockForResponse(message, true, left, right);
    }

    protected boolean blockForResponse(String message, boolean yesNo) {
        return this.blockForResponse(message, yesNo, "Yes", "No");
    }

    protected boolean blockForResponse(String message, boolean yesNo, String left, String right) {
        response = RESPONSE_NONE;
        askForResponse(message,  new YesNoListener() {
            public void no() {
                CommCareInitializer.this.response = CommCareInitializer.RESPONSE_NO;
            }
            public void yes() {
                CommCareInitializer.this.response = CommCareInitializer.RESPONSE_YES;
            }

        }, yesNo, left, right);
        while(response == RESPONSE_NONE);
        return response == RESPONSE_YES;
    }
}
