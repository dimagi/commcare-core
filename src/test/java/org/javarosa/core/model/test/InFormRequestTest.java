package org.javarosa.core.model.test;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.test_utils.MockFormSendCalloutHandler;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ctsims on 9/27/2017.
 */

public class InFormRequestTest {
    @Test
    public void testResponseWithParams() {
        FormParseInit fpi = new FormParseInit("/send_action/end_to_end_with_params.xml");
        FormDef form = fpi.getFormDef();

        form.setSendCalloutHandler(MockFormSendCalloutHandler.succeedWithArgAtKey("value_two"));
        FormEntryController fec = FormDefTest.initFormEntry(fpi);
        fec.stepToNextEvent();
        Assert.assertEquals("Response:two",fec.getQuestionPrompts()[0].getQuestionText());
    }

    @Test
    public void testResponseNull() {
        FormParseInit fpi = new FormParseInit("/send_action/end_to_end_with_params.xml");
        FormDef form = fpi.getFormDef();

        form.setSendCalloutHandler(MockFormSendCalloutHandler.nullResponse());
        FormEntryController fec = FormDefTest.initFormEntry(fpi);
        fec.stepToNextEvent();
        Assert.assertEquals("Response:",fec.getQuestionPrompts()[0].getQuestionText());
    }

    @Test
    public void testResponseError() {
        FormParseInit fpi = new FormParseInit("/send_action/end_to_end_with_params.xml");
        FormDef form = fpi.getFormDef();

        form.setSendCalloutHandler(MockFormSendCalloutHandler.withException());
        FormEntryController fec = FormDefTest.initFormEntry(fpi);
        fec.stepToNextEvent();
        Assert.assertEquals("Response:",fec.getQuestionPrompts()[0].getQuestionText());
    }

    @Test
    public void testWithNoParams() {
        FormParseInit fpi = new FormParseInit("/send_action/end_to_end_no_params.xml");
        FormDef form = fpi.getFormDef();

        form.setSendCalloutHandler(MockFormSendCalloutHandler.forSuccess("payloadvalue"));
        FormEntryController fec = FormDefTest.initFormEntry(fpi);
        fec.stepToNextEvent();
        Assert.assertEquals("Response:payloadvalue",fec.getQuestionPrompts()[0].getQuestionText());
    }

    @Test
    public void testWithEmptyParams() {
        FormParseInit fpi = new FormParseInit("/send_action/end_to_end_empty_params.xml");
        FormDef form = fpi.getFormDef();

        form.setSendCalloutHandler(MockFormSendCalloutHandler.forSuccess("payloadvalue"));
        FormEntryController fec = FormDefTest.initFormEntry(fpi);
        fec.stepToNextEvent();
        Assert.assertEquals("Response:payloadvalue",fec.getQuestionPrompts()[0].getQuestionText());
    }

    @Test
    public void testWithMissingParams() {
        FormParseInit fpi = new FormParseInit("/send_action/end_to_end_missing_params.xml");
        FormDef form = fpi.getFormDef();

        form.setSendCalloutHandler(MockFormSendCalloutHandler.forSuccess("payloadvalue"));
        FormEntryController fec = FormDefTest.initFormEntry(fpi);
        fec.stepToNextEvent();
        Assert.assertEquals("Response:payloadvalue",fec.getQuestionPrompts()[0].getQuestionText());
    }

    @Test
    public void testWithNoHandler() {
        FormParseInit fpi = new FormParseInit("/send_action/end_to_end_missing_params.xml");

        FormEntryController fec = FormDefTest.initFormEntry(fpi);
        fec.stepToNextEvent();
        Assert.assertEquals("Response:",fec.getQuestionPrompts()[0].getQuestionText());
    }
}
