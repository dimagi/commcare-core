/**
 * 
 */
package org.commcare.view;

import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import org.commcare.api.transitions.SuiteTransitions;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Filter;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.Localizer;
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
	Menu m;

	Entry[] indexMapping = new Entry[]{};
	
	public CommCareSuiteController(Suite suite, Menu m) {
		this.suite = suite;
		this.m = m;
		
		view = new CommCareSuiteView(m.getName().evaluate());
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
		indexMapping = new Entry[m.getCommandIds().size()];
		for(String id : m.getCommandIds()) {
			Entry entry = entries.get(id);
			int index = view.append(CommCareUtil.getEntryText(entry, suite), null);
			indexMapping[index] = entry;
		}
	}

	public void commandAction(Command c, Displayable d) {
		CrashHandler.commandAction(this, c, d);
	}  

	public void _commandAction(Command c, Displayable d) {
		if(c.equals(List.SELECT_COMMAND)) {
			Entry e = indexMapping[view.getSelectedIndex()];
			transitions.entry(suite, e);
		}
		else if(c.equals(CommCareSuiteView.BACK)) {
			transitions.exit();
		}
	}	
}
