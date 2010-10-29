/**
 * 
 */
package org.commcare.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.applogic.CommCareFormEntryState;
import org.commcare.applogic.CommCareHomeState;
import org.commcare.applogic.CommCareSelectState;
import org.commcare.applogic.MenuHomeState;
import org.commcare.entity.CaseInstanceLoader;
import org.commcare.entity.CommCareEntity;
import org.commcare.entity.FormDefInstanceLoader;
import org.commcare.entity.ReferralInstanceLoader;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.javarosa.cases.model.Case;
import org.javarosa.chsreferral.model.PatientReferral;
import org.javarosa.core.api.State;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.entity.model.Entity;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.utilities.media.MediaUtils;

import de.enough.polish.ui.List;

/**
 * The CommCare Session Controller is responsible for managing the
 * user experience of a CommCare session, from start until either
 * the session is canceled or the user arrives at a form entry
 * stage.
 * 
 * @author ctsims
 *
 */
public class CommCareSessionController {
	
	protected CommCareSession session;
	
	protected State currentState;
	
	//I hate this...
	private Hashtable<Integer, Suite> suiteTable = new Hashtable<Integer,Suite>();
	private Hashtable<Integer, Entry> entryTable = new Hashtable<Integer,Entry>();
	private Hashtable<Integer, Menu> menuTable = new Hashtable<Integer,Menu>();

	public CommCareSessionController(CommCareSession session, State currentState) {
		this.session = session;
		this.currentState = currentState;
	}
	
	public void populateMenu(List list, String menu) {
		suiteTable.clear();
		entryTable.clear();
		menuTable.clear();
		Enumeration en = session.platform.getInstalledSuites().elements();
		while(en.hasMoreElements()) {
			Suite suite = (Suite)en.nextElement();
			for(Menu m : suite.getMenus()) {
				if(menu.equals(m.getId())){
					for(String id : m.getCommandIds()) {
						Entry e = suite.getEntries().get(id);
						int location = list.append(CommCareUtil.getEntryText(e,suite), MediaUtils.getImage(e.getImageURI()));
						suiteTable.put(new Integer(location),suite);
						entryTable.put(new Integer(location),e);
					}
				}
				else if(m.getRoot().equals(menu)) {
					int location = list.append(m.getName().evaluate(), null);
					suiteTable.put(new Integer(location),suite);
					menuTable.put(new Integer(location),m);
				}
			}
		}
	}
	
	public Suite getSelectedSuite(int selectedItem) {
		Integer selected = new Integer(selectedItem);
		
		if(suiteTable.containsKey(selected)) {
			return suiteTable.get(selected);
		} else {
			return null;
		}
	}
	
	public Menu getSelectedMenu(int selectedItem) {
		Integer selected = new Integer(selectedItem);
		
		if(menuTable.containsKey(selected)) {
			return menuTable.get(selected);
		} else {
			return null;
		}
	}
	
	public Entry getSelectedEntry(int selectedItem) {
		Integer selected = new Integer(selectedItem);
		
		if(entryTable.containsKey(selected)) {
			return entryTable.get(selected);
		} else {
			return null;
		}
	}
	
	public void chooseSessionItem(int item) {
		Menu m = getSelectedMenu(item);
		if(m == null) {
			Entry e = getSelectedEntry(item);
			session.setCommand(e.getCommandId());
		} else {
			//We selected a menu, tell the session the command.
			session.setCommand(m.getId());
		}
	}
	
	public void next() {
		String next = session.getNeededData();
		if(next == null) {
			//create form entry session
			Entry entry = session.getEntriesForCommand(session.getCommand()).elementAt(0);
			String xmlns = session.getForm();
			CommCareFormEntryState state = new CommCareFormEntryState(Localizer.clearArguments(entry.getText().evaluate()),xmlns, getPreloaders(), CommCareContext._().getFuncHandlers()) {
				protected void goHome() {
					J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
				}
			};
			J2MEDisplay.startStateWithLoadingScreen(state);
			return;
		}
		
		if(next.equals(CommCareSession.STATE_COMMAND_ID)) {
			//You only get commands from menus, so the current 
			//command has to be a menu, we should load a menu state
			
			if(session.getCommand() == null) {
				//We're at the root selection, we need to go home
				session.clearState();
				J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
				return;
			}
			
			MenuHomeState state = new MenuHomeState(this, session.getMenu(session.getCommand())) {

				public void entry(Suite suite, Entry entry) {
					//This shouldn't happen anymore
					throw new RuntimeException("Clayton messed up");
				}

				public void exitMenuTransition() {
					CommCareSessionController.this.back();
				}
				
			};
			J2MEDisplay.startStateWithLoadingScreen(state);
			return;
		}
		
		
		//The rest of the selections all depend on the suite being available for checkin'
		Suite suite = session.getCurrentSuite();
		Entry entry = session.getEntriesForCommand(session.getCommand()).elementAt(0);		
		
		if(next.equals(CommCareSession.STATE_REFERRAL_ID)) {
			Entity<PatientReferral> entity = new CommCareEntity<PatientReferral>(suite.getDetail(entry.getShortDetailId()), suite.getDetail(entry.getLongDetailId()), new ReferralInstanceLoader(entry.getReferences()));
			CommCareSelectState<PatientReferral> select = new CommCareSelectState<PatientReferral>(entity,PatientReferral.STORAGE_KEY) {
				
				public void cancel() {
					CommCareSessionController.this.back();
				}
				
				public void entitySelected(int id) {
					PatientReferral r = CommCareUtil.getReferral(id);
					Case c = CommCareUtil.getCase(r.getLinkedId());
					CommCareSessionController.this.session.setReferral(r.getReferralId(), r.getType());
					CommCareSessionController.this.session.setCaseId(c.getCaseId());
					CommCareSessionController.this.next();

				}
			};
			J2MEDisplay.startStateWithLoadingScreen(select);
			return;
		}

		if(next.equals(CommCareSession.STATE_CASE_ID)) {
			Entity<Case> entity = new CommCareEntity<Case>(suite.getDetail(entry.getShortDetailId()), suite.getDetail(entry.getLongDetailId()), new CaseInstanceLoader(entry.getReferences()));
			CommCareSelectState<Case> select = new CommCareSelectState<Case>(entity,Case.STORAGE_KEY) {
				
				public void cancel() {
					CommCareSessionController.this.back();
				}
				
				public void entitySelected(int id) {
					Case c = CommCareUtil.getCase(id);
					CommCareSessionController.this.session.setCaseId(c.getCaseId());
					CommCareSessionController.this.next();

				}
			};
			J2MEDisplay.startStateWithLoadingScreen(select);
			return;
		}
				
		if(next.equals(CommCareSession.STATE_FORM_XMLNS)) {
			Entity<FormDef> entity = new CommCareEntity<FormDef>(suite.getDetail(entry.getShortDetailId()), suite.getDetail(entry.getLongDetailId()), new FormDefInstanceLoader(entry.getReferences()));
			CommCareSelectState<FormDef> select = new CommCareSelectState<FormDef>(entity,FormDef.STORAGE_KEY) {
				
				public void cancel() {
					CommCareSessionController.this.back();
				}
				
				public void entitySelected(int id) {
					FormDef r = CommCareUtil.getForm(id);
					CommCareSessionController.this.session.setXmlns(r.getInstance().schema);
					CommCareSessionController.this.next();

				}
			};
			J2MEDisplay.startStateWithLoadingScreen(select);
			return;
		}
	}

	private Vector<IPreloadHandler> getPreloaders() {
		String caseId = session.getCaseId();
		String referralId = session.getReferralId();
		String type = session.getReferralType();
		return CommCareContext._().getPreloaders(caseId == null ? null : CommCareUtil.getCase(caseId), referralId == null ? null : CommCareUtil.getReferral(referralId, type));
	}

	protected void back() {
		session.stepBack();
		next();
	}
}