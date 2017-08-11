package org.commcare.test.utilities;

import org.javarosa.engine.XFormPlayer;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.model.xform.XFormSerializingVisitor;

import java.io.IOException;

/**
 * Created by ctsims on 8/11/2017.
 */

public class XFormTestUtilities {

    public static byte[] finalizeAndSerializeForm(FormEntryController fec) {

        fec.getModel().getForm().postProcessInstance();

        XFormSerializingVisitor visitor = new XFormSerializingVisitor();
        try {
            return visitor.serializeInstance(fec.getModel().getForm().getInstance());
        } catch (IOException e) {
            RuntimeException re = new RuntimeException("Couldn't serialize form during tests");
            re.initCause(e);
            throw re;
        }
    }

}
