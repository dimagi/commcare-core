package org.commcare.backend.suite.model.test;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionDescriptorUtil;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.test.utilities.MockApp;
import org.commcare.test.utilities.PersistableSandbox;
import org.commcare.session.SessionFrame;

import org.commcare.util.FormDataUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by amstone326 on 8/7/15.
 */
public class StackFrameStepTests {

    private PersistableSandbox mSandbox;

    private StackFrameStep commandIdStepV1;
    private StackFrameStep commandIdStepV2;

    private StackFrameStep caseIdStepV1;
    private StackFrameStep caseIdStepV2;

    private StackFrameStep formXmlnsStepV1;
    private StackFrameStep formXmlnsStepV2;

    private StackFrameStep datumComputedStepV1;
    private StackFrameStep datumComputedStepV2;

    private StackFrameStep stepWithExtras;
    private StackFrameStep stepWithBadExtras;

    @Before
    public void setUp() {
        mSandbox = new PersistableSandbox();

        commandIdStepV1 = new StackFrameStep(SessionFrame.STATE_COMMAND_ID, "id", null);
        commandIdStepV2 = new StackFrameStep(SessionFrame.STATE_COMMAND_ID, "id", null);

        caseIdStepV1 = new StackFrameStep(SessionFrame.STATE_DATUM_VAL, "id", "case_val");
        caseIdStepV2 = new StackFrameStep(SessionFrame.STATE_DATUM_VAL, "id", null);

        formXmlnsStepV1 = new StackFrameStep(SessionFrame.STATE_FORM_XMLNS, "xmlns_id1", null);
        formXmlnsStepV2 = new StackFrameStep(SessionFrame.STATE_FORM_XMLNS, "xmlns_id2", null);

        datumComputedStepV1 = new StackFrameStep(SessionFrame.STATE_DATUM_COMPUTED, "datum_val_id", "datum_val1");
        datumComputedStepV2 = new StackFrameStep(SessionFrame.STATE_DATUM_COMPUTED, "datum_val_id", "datum_val2");

        // frame steps can store externalizable data, such as ints, Strings,
        // or anything that implements Externalizable
        stepWithExtras = new StackFrameStep(SessionFrame.STATE_DATUM_COMPUTED, "datum_val_id", "datum_val2");
        stepWithExtras.addExtra("key", 123);

        // Demonstrate how frame steps can't store non-externalizable data in extras
        stepWithBadExtras = new StackFrameStep(SessionFrame.STATE_DATUM_COMPUTED, "datum_val_id", "datum_val2");
        stepWithBadExtras.addExtra("key", new ByteArrayInputStream(new byte[]{1,2,3}));
    }

    @Test
    public void equalityTests() {
        assertTrue("Identical StackFrameSteps were not equal",
                commandIdStepV1.equals(commandIdStepV2));

        assertFalse("StackFrameStep was equal to null",
                commandIdStepV1 == null);

        assertFalse("StackFrameSteps with different command types were equal",
                commandIdStepV1.equals(caseIdStepV2));

        assertFalse("StackFrameSteps with different ids were equal",
                formXmlnsStepV1.equals(formXmlnsStepV2));

        assertFalse("StackFrameSteps with different values were equal",
                datumComputedStepV1.equals(datumComputedStepV2));

        assertFalse("StackFrameSteps where one value is null and one is non-null were equal",
                caseIdStepV1.equals(caseIdStepV2));
        assertFalse("StackFrameSteps where one value is null and one is non-null were equal",
                caseIdStepV2.equals(caseIdStepV1));
    }

    @Test
    public void serializationTest() {
        byte[] serializedStep = mSandbox.serialize(commandIdStepV1);
        StackFrameStep deserialized = mSandbox.deserialize(serializedStep, StackFrameStep.class);
        assertTrue("Serialization resulted in altered StackFrameStep",
                commandIdStepV1.equals(deserialized));

        serializedStep = mSandbox.serialize(stepWithExtras);
        deserialized = mSandbox.deserialize(serializedStep, StackFrameStep.class);
        assertTrue("",
                stepWithExtras.equals(deserialized));

        boolean failed = false;
        try {
            mSandbox.serialize(stepWithBadExtras);
        } catch (Exception e) {
            failed = true;
        }
        assertTrue(failed);
    }

    /**
     * Load form title from session where the case id is computed, not
     * selected, and the case name is loaded from detail referenced by m0-f0,
     * which is implicitly referenced
     */
    @Test
    public void loadRegistrationFormTitleFromSessionTest() throws Exception {
        MockApp mockApp = new MockApp("/case_title_form_loading/");
        SessionWrapper session = mockApp.getSession();
        UserSandbox sandbox = session.getSandbox();
        SessionWrapper blankSession = new SessionWrapper(session.getPlatform(), sandbox);
        String descriptor = "COMMAND_ID m0 "
                + "COMMAND_ID m3-f0 "
                + "CASE_ID case_id_new_adult_0 case_one "
                + "CASE_ID usercase_id 05c0fb7a77a54eed9872fc1b72a21826 "
                + "CASE_ID return_to m0";
        SessionDescriptorUtil.loadSessionFromDescriptor(descriptor, blankSession);
        blankSession.stepBack();
        assertEquals(SessionFrame.STATE_DATUM_VAL, blankSession.getNeededData());
        assertEquals(SessionFrame.STATE_COMMAND_ID, blankSession.getPoppedStep().getType());
    }
}
