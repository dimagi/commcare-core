/**
 *
 */
package org.commcare.util.time;

import org.commcare.applogic.CommCareUpgradeState;
import org.commcare.core.properties.CommCareProperties;
import org.commcare.model.PeriodicEvent;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.j2me.view.J2MEDisplay;

/**
 * The AutoUpdate event is responsible for checking for updates to CommCare's
 * resource tables. It should be run at a pace specified by the application's
 * properties.
 *
 * @author ctsims
 *
 */
public class AutoUpdateEvent extends PeriodicEvent {

    public static final String EVENT_KEY = "autoupdate_event";


    /* (non-Javadoc)
     * @see org.javarosa.core.api.State#start()
     */
    public void start() {
        CommCareUpgradeState state = new CommCareUpgradeState(false) {
            public void done() {
                AutoUpdateEvent.this.done();
            }
        };
        J2MEDisplay.startStateWithLoadingScreen(state);
    }

    /*
     * (non-Javadoc)
     * @see org.commcare.model.PeriodicEvent#getEventKey()
     */
    protected String getEventKey() {
        return EVENT_KEY;
    }

    /*
     * (non-Javadoc)
     * @see org.commcare.model.PeriodicEvent#getEventPeriod()
     */
    protected int getEventPeriod() {
        String frequency = PropertyManager._().getSingularProperty(CommCareProperties.AUTO_UPDATE_FREQUENCY);
        if(frequency == null) { frequency = CommCareProperties.FREQUENCY_NEVER;}
        if(frequency.equals(CommCareProperties.FREQUENCY_NEVER)) {
            return PeriodicEvent.TYPE_DISABLED;
        } else if(frequency.equals(CommCareProperties.FREQUENCY_DAILY)) {
            return PeriodicEvent.TYPE_DAILY;
        } else if(frequency.equals(CommCareProperties.FREQUENCY_WEEKLY)) {
            return PeriodicEvent.TYPE_WEEKLY;
        }

        //If we don't recognize it, don't do anything.
        return PeriodicEvent.TYPE_DISABLED;
    }

}
