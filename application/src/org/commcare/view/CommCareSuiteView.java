/**
 * 
 */
package org.commcare.view;

import javax.microedition.lcdui.Command;

import de.enough.polish.ui.List;

/**
 * @author ctsims
 *
 */
public class CommCareSuiteView extends List{	
	public final static Command BACK = new Command("Back", Command.BACK, 0);
	
	public CommCareSuiteView(String title) {
		super(title, List.IMPLICIT);
		this.addCommand(BACK);
	}
}
