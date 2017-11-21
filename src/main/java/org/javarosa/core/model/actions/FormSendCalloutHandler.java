package org.javarosa.core.model.actions;

import java.util.Map;

/**
 * A platform-specific handler attached to a FormDef which provides the form with
 * a mechanism to field basic <submission/> callouts.
 *
 * Created by ctsims on 9/27/2017.
 */

public interface FormSendCalloutHandler {

    /**
     * Attempt a synchronous callout to the provided url with the provided parameters.
     *
     * This method can respond with null if an expected error occurs (like a network timeout), or
     * can throw a runtime exception if the inputs are unexpected, but if a String is returned
     * (even an empty string), it must be the content body of a 200 success response.
     *
     * note: Neither input is specifically scrubbed for url encoding
     *
     * @return
     */
    String performHttpCalloutForResponse(String url, Map<String, String> paramMap);
}
