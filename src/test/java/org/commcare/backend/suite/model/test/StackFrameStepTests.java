package org.commcare.backend.suite.model.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;

import org.cli.MockSessionUtils;
import org.commcare.core.interfaces.MemoryVirtualDataInstanceStorage;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.test.utilities.MockApp;
import org.commcare.test.utilities.PersistableSandbox;
import org.commcare.util.screen.QueryScreen;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Vector;

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
        stepWithExtras.addExtra("key", 234);

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
     * Confirm that when stepping back after a stack push, we remove all pushed data
     */
    @Test
    public void stepBackFromStackPush() throws Exception {
        MockApp mApp = new MockApp("/case_title_form_loading/");
        SessionWrapper session = mApp.getSession();
        session.setCommand("m0");
        session.setComputedDatum();
        EntityDatum entityDatum = (EntityDatum) session.getNeededDatum();
        Vector<Action> actions = session.getDetail(entityDatum.getShortDetail()).getCustomActions(session.getEvaluationContext());
        if (actions == null || actions.isEmpty()) {
            Assert.fail("Detail screen stack action was missing from app!");
        }
        //We're using the second action for this screen which requires us to still need another datum
        Action dblManagement = actions.elementAt(1);
        assertEquals(1, session.getFrame().getSteps().size());
        session.executeStackOperations(dblManagement.getStackOperations(), session.getEvaluationContext());
        assertEquals(5, session.getFrame().getSteps().size());
        session.stepBack();
        assertEquals(1, session.getFrame().getSteps().size());
    }

    /**
     * Test that stacks with queries in them work and preserve all the data elements they contain
     * even if they have duplicate keys.
     */
    @Test
    public void stackWithQueries() throws Exception {
        MockApp mApp = new MockApp("/queries_in_entry_and_stack/");
        SessionWrapper session = mApp.getSession();
        session.setCommand("m0-f0");

        // check that session datum requirement is satisfied
        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        assertEquals("case_id", session.getNeededDatum().getDataId());
        session.setEntityDatum("case_id", "case_one");

        assertEquals(SessionFrame.STATE_QUERY_REQUEST, session.getNeededData());
        // construct the screen
        // mock the query response
        InputStream response = this.getClass().getResourceAsStream("/case_claim_example/query_response.xml");
        QueryScreen screen = new QueryScreen(
                "username", "password",
                System.out, new MemoryVirtualDataInstanceStorage(), new MockSessionUtils(response));
        screen.init(session);

        // perform the query
        boolean success = screen.handleInputAndUpdateSession(session, "", false, null, true);
        Assert.assertTrue(success);

        assertNull(session.getNeededDatum());

        // execute entry stack
        session.finishExecuteAndPop(session.getEvaluationContext());

        // validate that the query step has all the correct entries (including 2 case_id entries)
        Vector<StackFrameStep> steps = session.getFrame().getSteps();
        assertEquals(4, steps.size());
        StackFrameStep queryFrame = steps.get(2);
        assertEquals(SessionFrame.STATE_QUERY_REQUEST, queryFrame.getElementType());

        ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
        builder.put("case_type", "patient");
        builder.put("x_commcare_data_registry", "test");
        builder.putAll("case_id", ImmutableList.of("case_one", "dupe1"));
        assertEquals(builder.build(), queryFrame.getExtras());
    }
}
