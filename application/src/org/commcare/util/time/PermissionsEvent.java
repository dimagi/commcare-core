/**
 *
 */
package org.commcare.util.time;

import org.commcare.model.PeriodicEvent;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.j2me.view.J2MEDisplay;

import de.enough.polish.midp.ui.Form;
import de.enough.polish.ui.Command;
import de.enough.polish.ui.CommandListener;
import de.enough.polish.ui.Displayable;

/**
 * A time message event displays a message to the user instructing them that
 * their device permissions are set incorrectly.
 *
 * It is a scheduled, daily event, meaning it should only occur when the system
 * explicitly schedules one, and should not occur more than once a day.
 *
 * @author ctsims
 *
 */
public class PermissionsEvent extends PeriodicEvent implements CommandListener {

    public static final String EVENT_KEY = "permission_event";

    public static Command BACK;


    /* (non-Javadoc)
     * @see org.javarosa.core.api.State#start()
     */
    public void start() {
        BACK = new Command(Localization.get("command.ok"), Command.OK, 0);
        Form display = new Form(Localization.get("intro.title"));
        display.addCommand(BACK);
        display.append(Localization.get("message.permissions"));
        display.setCommandListener(this);

        J2MEDisplay.setView(display);
        Logger.log("permissions_notify", "Displayed permissions message");
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
        return PeriodicEvent.TYPE_FLAG_SCHEDULED  | PeriodicEvent.TYPE_DAILY;
    }

    /*
     * (non-Javadoc)
     * @see de.enough.polish.ui.CommandListener#commandAction(de.enough.polish.ui.Command, de.enough.polish.ui.Displayable)
     */
    public void commandAction(Command c, Displayable d) {
        this.done();
    }

}
