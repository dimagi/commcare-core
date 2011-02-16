/**
 * 
 */
package org.commcare.util.time;

import org.commcare.model.PeriodicEvent;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.j2me.view.J2MEDisplay;

import de.enough.polish.midp.ui.Form;
import de.enough.polish.ui.Command;
import de.enough.polish.ui.CommandListener;
import de.enough.polish.ui.Displayable;


/**
 * @author ctsims
 *
 */
public class TimeMessageEvent extends PeriodicEvent implements CommandListener {
	
	public static final String EVENT_KEY = "time_sync";
	
	public final static Command BACK = new Command(Localization.get("command.ok"), Command.OK, 0);


	/* (non-Javadoc)
	 * @see org.javarosa.core.api.State#start()
	 */
	public void start() {
		Form display = new Form(Localization.get("intro.title"));
		display.addCommand(BACK);
		display.append(Localization.get("message.timesync"));
		display.setCommandListener(this);
		
		J2MEDisplay.setView(display);
	}

	protected String getEventKey() {
		return EVENT_KEY;
	}

	protected int getEventPeriod() {
		return PeriodicEvent.TYPE_SCHEDULED  | PeriodicEvent.TYPE_DAILY;
	}

	public void commandAction(Command c, Displayable d) {
		this.done();
	}

}
