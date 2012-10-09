/**
 * 
 */
package org.commcare.restore;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;

import org.javarosa.core.services.locale.Localization;

import de.enough.polish.ui.Form;
import de.enough.polish.ui.StringItem;
import de.enough.polish.ui.UiAccess;

/**
 * @author ctsims
 *
 */
public class CommCareOTARestoreView extends Form{

	public final Command FINISHED = new Command(Localization.get("restore.finished"),Command.SCREEN,1); 
	
	
	
	StringItem details;
	String buffer;
	
	boolean finished;

	public CommCareOTARestoreView(String title) {
		super(title);
		details = new StringItem("","");
		this.append(details);
		buffer = "";
	}
	
	public void addToMessage(String message) {
		buffer += message + "\n";
		setMessage(buffer);
	}
	
	public void setMessage(String message) {
		buffer = message;
		details.setText(buffer);	
	}
	
	public void setFinished() {
		this.addCommand(FINISHED);
		//Scroll to the bottom
		this.setScrollYOffset(this.getScreenFullHeight() - this.contentHeight, true);
		this.finished = true;
	}
	
	protected boolean handleKeyReleased(int keyCode, int gameAction) {
		if(super.handleKeyReleased(keyCode, gameAction)) {
			//Don't do anything that already does something.
			return true;
		} else {
			if(finished) {
				//scrolling should be uninterrupted
				switch(gameAction) {
				case Canvas.UP:
				case Canvas.DOWN:
				case Canvas.LEFT:
				case Canvas.RIGHT:
					return false;
				default:
					UiAccess.cast(this.getCommandListener()).commandAction(FINISHED,this);
					return true;
				}
			}
			return false;
		}
	}
}
