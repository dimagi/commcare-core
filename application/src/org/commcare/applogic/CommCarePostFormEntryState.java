package org.commcare.applogic;

import org.commcare.services.AutomatedSenderService;
import org.commcare.util.CommCareHQResponder;
import org.commcare.util.CommCareSense;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.formmanager.api.CompletedFormOptionsController;
import org.javarosa.formmanager.api.CompletedFormOptionsState;
import org.javarosa.j2me.view.J2MEDisplay;

public class CommCarePostFormEntryState extends CompletedFormOptionsState {

    boolean cacheAutomatically = false;

    public CommCarePostFormEntryState (String messageId, boolean cacheAutomatically) {
        super(messageId);
        this.cacheAutomatically = cacheAutomatically;
    }

    protected CompletedFormOptionsController getController () {
        return new CompletedFormOptionsController(messageId, cacheAutomatically);
    }

    /* (non-Javadoc)
     * @see org.javarosa.formmanager.api.transitions.CompletedFormOptionsStateTransitions#sendData(org.javarosa.core.model.instance.FormInstance)
     */
    public void sendData(String messageId) {

        // The message needs to already be on the queue at this point, so we'll actually just trigger sending all unsent (rather than
        // just trying to send the individual form)

        J2MEDisplay.startStateWithLoadingScreen(new SendAllUnsentState () {
            protected SendAllUnsentController getController () {
                return new SendAllUnsentController(new CommCareHQResponder(PropertyManager._().getSingularProperty(JavaRosaPropertyRules.OPENROSA_API_LEVEL)));
            }

            public void done() {
                notifyUnsent();
                goHome();
            }
        });
    }

    public void notifyUnsent() {
        //If we're autosending, make sure to expire old deadlines
                if(CommCareSense.isAutoSendEnabled()) {

                    //Notify the service that old deadlines have expired.
                    AutomatedSenderService.NotifyPending();
                }
    }

    /* (non-Javadoc)
     * @see org.javarosa.formmanager.api.transitions.CompletedFormOptionsStateTransitions#skipSend()
     */
    public void skipSend(String messageId) {
        // We're now relying on the form processor to have cached the message already, since otherwise
        // we may end up processing but not caching/sending the form.
        Logger.log("transport", "Defer[" + messageId + "]");
        notifyUnsent();
        goHome();
    }

    public void goHome() {
        J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
    }
}
