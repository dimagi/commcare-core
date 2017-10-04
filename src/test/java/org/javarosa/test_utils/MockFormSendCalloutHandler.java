package org.javarosa.test_utils;

import org.javarosa.core.model.actions.FormSendCalloutHandler;

import java.util.Map;

/**
 * Mocks the responses needed for form <submission/> responses in tests
 *
 * Created by ctsims on 9/27/2017.
 */

public class MockFormSendCalloutHandler implements FormSendCalloutHandler {
    String payload;
    boolean throwException = false;
    String argreturn;

    private MockFormSendCalloutHandler(String payload, boolean throwException) {
        this.payload = payload;
        this.throwException = throwException;
    }

    public static MockFormSendCalloutHandler forSuccess(String payload) {
        return new MockFormSendCalloutHandler(payload, false);
    }

    public static MockFormSendCalloutHandler succeedWithArgAtKey(String argreturn) {
        MockFormSendCalloutHandler handler = new MockFormSendCalloutHandler(null, false);
        handler.argreturn = argreturn;
        return handler;
    }


    public static MockFormSendCalloutHandler withException() {
        return new MockFormSendCalloutHandler(null, true);
    }

    public static MockFormSendCalloutHandler nullResponse() {
        return new MockFormSendCalloutHandler(null, false);
    }

    @Override
    public String performHttpCalloutForResponse(String url, Map<String, String> paramMap) {
        if(throwException) {
            throw new RuntimeException("Expected Http Callout Exception");
        }else if(argreturn != null) {
            return paramMap.get(argreturn);
        } else {
            return payload;
        }
    }
}
