package org.commcare.applogic;

import java.io.IOException;

import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareHQResponder;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.formmanager.api.FormTransportState;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.services.transport.TransportMessage;

public abstract class CommCareFormSendState extends FormTransportState {

    public CommCareFormSendState (TransportMessage message) throws IOException {
        super(message,new CommCareHQResponder(PropertyManager._().getSingularProperty(JavaRosaPropertyRules.OPENROSA_API_LEVEL)));
    }

    public void sendToBackground() {
        done();
    }
}