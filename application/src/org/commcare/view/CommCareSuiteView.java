/**
 * 
 */
package org.commcare.view;

import javax.microedition.lcdui.Command;

import org.javarosa.core.services.locale.Localization;

import de.enough.polish.ui.List;
import de.enough.polish.ui.TextField;

/**
 * @author ctsims
 *
 */
public class CommCareSuiteView extends List{	
	public final static Command BACK = new Command(Localization.get("command.back"), Command.BACK, 0);
	
	public CommCareSuiteView(String title) {
		super(title, List.IMPLICIT);
		this.addCommand(BACK);
		TextField f = null;
	}
}
