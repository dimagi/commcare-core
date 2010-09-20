/**
 * 
 */
package org.commcare.view;

import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
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
import org.javarosa.formmanager.view.chatterbox.widget.ExpandedWidget;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.utilities.media.MediaUtils;

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
	}
	
	public void setTransitions (SuiteTransitions transitions) {
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
	
	private void configView() {
		Hashtable<String, Entry> entries = suite.getEntries();
		indexMapping = new Entry[m.getCommandIds().size()];
		for(String id : m.getCommandIds()) {
			Entry entry = entries.get(id);
			
			System.out.println(entry.getImageURI()+", for entry:"+getText(entry));
			if(entry.getImageURI() != null) System.out.println("Attempting to get label image for form:"+getText(entry)+", image:"+entry.getImageURI());
			
			Image im = MediaUtils.getImage(entry.getImageURI());
			int index = view.append(getText(entry), MediaUtils.getImage(entry.getImageURI()));
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
	
	/**
	 * Gets the text associated with this entry, while dynamically evaluating
	 * and resolving any necessary count arguments that might need to be 
	 * included. 
	 * 
	 * @param entry
	 * @return
	 */
	private String getText(final Entry entry) {
		String text = entry.getText().evaluate();
		if(Localizer.getArgs(text).size() == 0) {
			return text;
		}
		else if(Localizer.getArgs(text).size() > 1) {
			//We really don't know how to deal with this yet. Shouldn't happen!
			return text;
		} else {
			//Sweet spot! This argument should be the count of all entities
			//which are possible inside of its selection.
			String wrapper = Localization.get("commcare.numwrapper"); 
			String wrapped = Localizer.processArguments(wrapper, new String[] { String.valueOf(CommCareUtil.countEntities(entry, suite)) });
			return Localizer.processArguments(text, new String[] {wrapped} );
		}
	}
	

}
