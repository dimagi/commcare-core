package org.commcare.backend.suite.model.test;

import org.commcare.suite.model.StackFrameStep;
import org.commcare.test.utilities.PersistableSandbox;
import org.commcare.session.SessionFrame;

import org.junit.Before;
import org.junit.Test;

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
    }

    @Test
    public void equalityTests() {
        assertTrue("Identical StackFrameSteps were not equal",
                commandIdStepV1.equals(commandIdStepV2));

        assertFalse("StackFrameStep was equal to null",
                commandIdStepV1.equals(null));

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
    }
}
