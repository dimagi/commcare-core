/**
 *
 */
package org.commcare.util;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.suite.model.Profile;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.formmanager.properties.FormManagerProperties;

/**
 * General placeholder class for CommCare Sense UI utility functions.
 *
 * Also holds tests which are always positive if sense is enabled.
 *
 * @author ctsims
 *
 */
public class CommCareSense {

    public static boolean sense() {
        return CommCareContext._().getManager().getCurrentProfile().isFeatureActive("sense");
    }

    public static boolean isAutoSendEnabled() {
        if(sense()) {
            if(CommCareProperties.SEND_UNSENT_MANUAL.equals(PropertyManager._().getSingularProperty(CommCareProperties.SEND_UNSENT_STYLE))){
                return false;
            }
            //Default: Send in background
            return true;
        } else {
            //Default: Manual
            return CommCareProperties.SEND_UNSENT_AUTOMATIC.equals(PropertyManager._().getSingularProperty(CommCareProperties.SEND_UNSENT_STYLE));
        }
    }

    public static String formEntryExtraKey() {
        if(sense()) { return FormManagerProperties.EXTRA_KEY_AUDIO_PLAYBACK; }
        return PropertyManager._().getSingularProperty(FormManagerProperties.EXTRA_KEY_FORMAT);

    }

    public static boolean formEntryQuick() {
        if(sense()) { return false; }
        else {
            return !CommCareProperties.ENTRY_MODE_REVIEW.equals(PropertyManager._().getSingularProperty(CommCareProperties.ENTRY_MODE));
        }
    }

    public static boolean isAutoLoginEnabled() {
        if(!CommCareContext._().getManager().getCurrentProfile().isFeatureActive(Profile.FEATURE_USERS)) { return false;}
        String loginMode = PropertyManager._().getSingularProperty(CommCareProperties.LOGIN_MODE);
        if(loginMode == null) { return sense();}
        else { return CommCareProperties.LOGIN_MODE_AUTO.equals(loginMode);}
    }
}
