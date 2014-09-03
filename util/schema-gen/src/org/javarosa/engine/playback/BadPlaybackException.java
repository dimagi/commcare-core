/**
 * 
 */
package org.javarosa.engine.playback;

import org.javarosa.engine.models.ActionResponse;
import org.javarosa.form.api.FormEntryPrompt;

/**
 * @author ctsims
 *
 */
public class BadPlaybackException extends Exception {
    public BadPlaybackException(String message) {
        super(message);
    }

    public BadPlaybackException(FormEntryPrompt fep, int response, ActionResponse actionResponse) {
        
    }
}
