/**
 * 
 */
package org.commcare.view;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import org.commcare.api.transitions.SuiteTransitions;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Suite;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;

/**
 * @author ctsims
 *
 */
public class CommCareSuiteController implements HandledCommandListener {

	CommCareSuiteView view;
	SuiteTransitions transitions;
	
	Suite suite;

	Entry[] indexMapping = new Entry[]{};
	
	public CommCareSuiteController(Suite suite) {
		this.suite = suite;
		
		view = new CommCareSuiteView("Select Action");
		configView();
		view.setCommandListener(this);
	}
	
	public void setTransitions (SuiteTransitions transitions) {
		this.transitions = transitions;
	}

	public void start() {
		J2MEDisplay.setView(view);
	}
	
	private void configView() {
		Hashtable<String, Entry> entries = suite.getEntries();
		indexMapping = new Entry[entries.size()];
		for(Enumeration en = entries.elements(); en.hasMoreElements() ; ) {
			Entry entry = (Entry)en.nextElement();
			int index = view.append(entry.getText().evaluate(), null);
			indexMapping[index] = entry;
		}
	}

	public void commandAction(Command c, Displayable d) {
		CrashHandler.commandAction(this, c, d);
	}  

	public void _commandAction(Command c, Displayable d) {
		if(c.equals(List.SELECT_COMMAND)) {
			Entry e = indexMapping[view.getSelectedIndex()];
			transitions.entry(e);
		}
		else if(c.equals(CommCareSuiteView.BACK)) {
			transitions.exit();
		}
	}
}
