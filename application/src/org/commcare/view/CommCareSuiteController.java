/**
 * 
 */
package org.commcare.view;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import org.commcare.api.transitions.MenuTransitions;
import org.commcare.suite.model.Menu;
import org.commcare.util.CommCareSessionController;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;

/**
 * @author ctsims
 *
 */
public class CommCareSuiteController implements HandledCommandListener {

	CommCareSuiteView view;
	MenuTransitions transitions;
	CommCareSessionController controller;
	
	Menu m;
	public CommCareSuiteController(CommCareSessionController controller, Menu m) {
		this.m = m;
		this.controller = controller;
		
		view = new CommCareSuiteView(m.getName().evaluate());
		view.setCommandListener(this);
		controller.populateMenu(view, m.getId());
	}
	
	public void setTransitions (MenuTransitions transitions) {
		this.transitions = transitions;
	}

	public void start() {
		//csims@dimagi.com - Aug 11, 2010 - Moved view creation
		//and instantiation logic here, since there's no good
		//way to return from another state without doing so if
		//the values determined in getText() change.
		view = new CommCareSuiteView(m.getName().evaluate());
		view.setCommandListener(this);
		configView();
		J2MEDisplay.setView(view);
	}
	
//	private void configView() {
//		Hashtable<String, Entry> entries = suite.getEntries();
//		indexMapping = new Entry[m.getCommandIds().size()];
//		for(String id : m.getCommandIds()) {
//			Entry entry = entries.get(id);
//			int index = view.append(CommCareUtil.getEntryText(entry, suite), null);
//			indexMapping[index] = entry;
//		}
//	}

	public void commandAction(Command c, Displayable d) {
		CrashHandler.commandAction(this, c, d);
	}  

	public void _commandAction(Command c, Displayable d) {
		if(c.equals(List.SELECT_COMMAND)) {
			controller.chooseSessionItem(view.getSelectedIndex());
			controller.next();
		}
		else if(c.equals(CommCareSuiteView.BACK)) {
			transitions.exitMenuTransition();
		}
	}	
}
