package org.commcare.applogic;

import java.io.IOException;

import org.commcare.util.CommCareContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.transport.payload.IDataPayload;
import org.javarosa.formmanager.api.CompletedFormOptionsState;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportException;

public abstract class CommCarePostFormEntryState extends CompletedFormOptionsState {

	public CommCarePostFormEntryState (FormInstance data) {
		super(data);
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.formmanager.api.transitions.CompletedFormOptionsStateTransitions#sendData(org.javarosa.core.model.instance.FormInstance)
	 */
	public void sendData(FormInstance data) {
		try {
			J2MEDisplay.startStateWithLoadingScreen(new CommCareFormSendState(data) {
				public void done() {
					CommCarePostFormEntryState.this.goHome();
				}
			});
		} catch (IOException e) {
			Logger.exception("CommCarePostFormEntryState.sendData", e);
			J2MEDisplay.showError(Localization.get("sending.status.error"), Localization.get("sending.status.error"));
			goHome();
		}
	}

	/* (non-Javadoc)
	 * @see org.javarosa.formmanager.api.transitions.CompletedFormOptionsStateTransitions#sendToFreshLocation(org.javarosa.core.model.instance.FormInstance)
	 */
	public void sendToFreshLocation(FormInstance data) {
		sendData(data);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.formmanager.api.transitions.CompletedFormOptionsStateTransitions#skipSend()
	 */
	public void skipSend(FormInstance data) {
		try {
			Logger.log("send", "later");
			IDataPayload payload = new XFormSerializingVisitor().createSerializedPayload(data);
			TransportService.send(CommCareContext._().buildMessage(payload), 0, 0);
			goHome();
		} catch (IOException e) {
			Logger.exception("CommCarePostFormEntryState.skipSend", e);
			J2MEDisplay.showError(Localization.get("sending.status.error"), "Form could not be serialized and can't be sent");
			goHome();
		} catch (TransportException e) {
			Logger.exception("CommCarePostFormEntryState.skipSend", e);
			J2MEDisplay.showError(Localization.get("sending.status.error"), Localization.get("sending.status.error"));
			goHome();
		}
	}
	
	public abstract void goHome();
}
