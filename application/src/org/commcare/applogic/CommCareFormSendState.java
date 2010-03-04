package org.commcare.applogic;

import java.io.IOException;

import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareHQResponder;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.formmanager.api.FormTransportState;
import org.javarosa.model.xform.XFormSerializingVisitor;

public abstract class CommCareFormSendState extends FormTransportState {
	
	public CommCareFormSendState (FormInstance data) throws IOException {
		super(CommCareContext._().buildMessage(new XFormSerializingVisitor().createSerializedPayload(data)),
			  new CommCareHQResponder());
	}

	public void sendToBackground() {
		done();
	}
}