package org.commcare.applogic;

import java.io.IOException;

import org.commcare.util.CommCareContext;
import org.javarosa.core.model.instance.FormInstance;
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
			new CommCareFormSendState(data) {
				public void done() {
					CommCarePostFormEntryState.this.goHome();
				}
			}.start();
		} catch (IOException e) {
			goHome();
			J2MEDisplay.showError(Localization.get("sending.status.error"), Localization.get("sending.status.error"));
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
			IDataPayload payload = new XFormSerializingVisitor().createSerializedPayload(data);
			TransportService.send(CommCareContext._().buildMessage(payload), 0, 0);
			goHome();
		} catch (IOException e) {
			goHome();
			J2MEDisplay.showError(Localization.get("sending.status.error"), "Form could not be serialized and can't be sent");
		} catch (TransportException e) {
			goHome();
			J2MEDisplay.showError(Localization.get("sending.status.error"), Localization.get("sending.status.error"));
		}
	}
	
	public abstract void goHome();
}
