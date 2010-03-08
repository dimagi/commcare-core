package org.commcare.view;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.services.locale.Localization;

import de.enough.polish.ui.ChoiceItem;
import de.enough.polish.ui.Command;
import de.enough.polish.ui.List;

/**
 * @author Clayton Sims
 * @date Jan 23, 2009 
 *
 */
public class CommCareHomeScreen extends List {
	CommCareHomeController controller;
	
	public ChoiceItem sendAllUnsent = new ChoiceItem(Localization.get("menu.send.all"), null, List.IMPLICIT);  

	public Command select = new Command("Select", Command.ITEM, 1);
	public Command exit = new Command("Exit", Command.EXIT, 1);
	public Command admNewUser = new Command("New User", Command.ITEM, 1);
	public Command admSettings = new Command("Settings", Command.ITEM, 1);
	public Command admFeedbackReport = new Command("Report Feedback", Command.ITEM, 1);
	public Command admEditUsers = new Command("Edit Users", Command.ITEM, 1);
	public Command admDeletePatient = new Command("Delete Patient", Command.ITEM, 1);
	public Command admBackupRestore = new Command("Backup/Restore", Command.ITEM, 1);
	public Command admJunkInDaTrunk = new Command("Generate Junk", Command.ITEM, 1);
	public Command admResetDemo = new Command("Reset Demo", Command.ITEM, 1);
	
	//I hate this...
	private Hashtable<Integer, Suite> suiteTable = new Hashtable<Integer,Suite>();
	private Hashtable<Integer, Entry> entryTable = new Hashtable<Integer,Entry>();
	private Hashtable<Integer, Menu> menuTable = new Hashtable<Integer,Menu>();

	public CommCareHomeScreen(CommCareHomeController controller, Vector<Suite> suites, boolean adminMode) {
		super("CommCare", List.IMPLICIT);
		this.controller = controller;
		
		Enumeration en = suites.elements();
		while(en.hasMoreElements()) {
			Suite suite = (Suite)en.nextElement();
			for(Menu m : suite.getMenus()) {
				if("root".equals(m.getId())){
					for(String id : m.getCommandIds()) {
						Entry e = suite.getEntries().get(id);
						int location = append(e.getText().evaluate(), null);
						suiteTable.put(new Integer(location),suite);
						entryTable.put(new Integer(location),e);
					}
				}
				else if(m.getRoot().equals("root")) {
					int location = append(m.getName().evaluate(), null);
					suiteTable.put(new Integer(location),suite);
					menuTable.put(new Integer(location),m);
				}
			}
		}

		append(sendAllUnsent);
		setSendUnsent();

		setCommandListener(controller);
		setSelectCommand(select);
		
		addCommand(exit);
		if (adminMode) {
			addCommand(admSettings);
			addCommand(admNewUser);
			addCommand(admEditUsers);
			addCommand(admBackupRestore);
			addCommand(admResetDemo);
		}
	}

	public void setSendUnsent() {
		String numunsent = "error"; 
		numunsent = String.valueOf(CommCareUtil.getNumberUnsent());
		sendAllUnsent.setText(Localization.get("menu.send.all.val", new String[] {numunsent}));
	}
	
	public Suite getSelectedSuite() {
		Integer selected = new Integer(this.getSelectedIndex());
		
		if(suiteTable.containsKey(selected)) {
			return suiteTable.get(selected);
		} else {
			return null;
		}
	}
	
	public Menu getSelectedMenu() {
		Integer selected = new Integer(this.getSelectedIndex());
		
		if(menuTable.containsKey(selected)) {
			return menuTable.get(selected);
		} else {
			return null;
		}
	}
	
	public Entry getSelectedEntry() {
		Integer selected = new Integer(this.getSelectedIndex());
		
		if(entryTable.containsKey(selected)) {
			return entryTable.get(selected);
		} else {
			return null;
		}
	}
}
