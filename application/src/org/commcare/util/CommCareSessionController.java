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
import org.commcare.core.properties.CommCareProperties;
import org.commcare.entity.CommCareEntity;
import org.commcare.entity.NodeEntitySet;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.Suite;
import org.javarosa.core.api.State;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.entity.model.Entity;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.model.xform.XPathReference;
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
		populateMenu(list,menu,null);
	}
	
	public void populateMenu(List list, String menu, MultimediaListener listener) {
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
						//TODO: All these multiple checks are pretty sloppy
						if(listener != null && (e.getAudioURI() != null && !"".equals(e.getAudioURI()))) {
							listener.registerAudioTrigger(location, e.getAudioURI());
						}
						suiteTable.put(new Integer(location),suite);
						entryTable.put(new Integer(location),e);
					}
				}
				else if(m.getRoot().equals(menu)) {
					int location = list.append(m.getName().evaluate(),  MediaUtils.getImage(m.getImageURI()));
					//TODO: All these multiple checks are pretty sloppy
					if(listener != null && (m.getAudioURI() != null && !"".equals(m.getAudioURI()))) {
						listener.registerAudioTrigger(location, m.getAudioURI());
					}
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
			String xmlns = session.getForm();
			
			if(xmlns == null) {
				//This command is a view, not an entry. We can comfortably return to the previous step.
				this.back();
				return;
			}
			//create form entry session
			Entry entry = session.getEntriesForCommand(session.getCommand()).elementAt(0);
			String title;
			if(CommCareSense.sense()) {
				title = null;
			} else {
				title = Localizer.clearArguments(entry.getText().evaluate());
			}
			CommCareFormEntryState state = new CommCareFormEntryState(title,xmlns, getPreloaders(), CommCareContext._().getFuncHandlers(), getIif()) {
				protected void goHome() {
					J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
				}
				public void abort () {
					 back();
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
		
		final SessionDatum datum = entry.getSessionDataReqs().elementAt(session.getData().size());
		
		
		Detail shortDetail = suite.getDetail(datum.getShortDetail());
		Detail longDetail = null;
		if(datum.getLongDetail() != null) {
			longDetail = suite.getDetail(datum.getLongDetail());
		}
		
		final EvaluationContext context = getEvaluationContext(shortDetail.getInstances());
		final NodeEntitySet nes = new NodeEntitySet(datum.getNodeset(), context);
		Entity<TreeReference> entity = new CommCareEntity(shortDetail, longDetail, context, nes);
		
		CommCareSelectState<TreeReference> select = new CommCareSelectState<TreeReference>(entity, nes) {
			public void cancel() {
				CommCareSessionController.this.back();
			}
			
			public void entitySelected(int id) {
				TreeReference selected = nes.get(id);
				TreeReference outcome = XPathReference.getPathExpr(datum.getValue()).getReference().contextualize(selected);
				AbstractTreeElement element = context.resolveReference(outcome);
				if(element == null) {
					throw new RuntimeException("No reference resolved for: " + outcome.toString());
				}
				CommCareSessionController.this.session.setDatum(datum.getDataId(), element.getValue().uncast().getString());
				CommCareSessionController.this.next();
			}
		};
		
		J2MEDisplay.startStateWithLoadingScreen(select, select.getProgressIndicator());
		return;
	}
	
	private InstanceInitializationFactory getIif() {
		return new CommCareInstanceInitializer(this);
	}

	private Vector<IPreloadHandler> getPreloaders() {
//		String caseId = session.getCaseId();
//		String referralId = session.getReferralId();
//		String type = session.getReferralType();
//		return CommCareContext._().getPreloaders(caseId == null ? null : CommCareUtil.getCase(caseId), referralId == null ? null : CommCareUtil.getReferral(referralId, type));
		return CommCareContext._().getPreloaders();
	}

	protected void back() {
		session.stepBack();
		next();
	}
	
	protected FormInstance getSessionInstance() {
		TreeElement sessionRoot = new TreeElement("session",0);
		
		TreeElement sessionData = new TreeElement("data",0);
		
		sessionRoot.addChild(sessionData);
		
		for(String[] step : session.steps) {
			if(step[0] == CommCareSession.STATE_DATUM_VAL) {
				TreeElement datum = new TreeElement(step[1]);
				datum.setValue(new UncastData(step[2]));
				sessionData.addChild(datum);
			}
		}
		
		TreeElement sessionMeta = new TreeElement("context",0);

		addData(sessionMeta, "deviceid", PropertyManager._().getSingularProperty(JavaRosaPropertyRules.DEVICE_ID_PROPERTY));
		addData(sessionMeta, "appversion", PropertyManager._().getSingularProperty(CommCareProperties.COMMCARE_VERSION));
		addData(sessionMeta, "username", CommCareContext._().getUser().getUsername());
		addData(sessionMeta, "userid", CommCareContext._().getUser().getUniqueId());

		sessionRoot.addChild(sessionMeta);
		
		return new FormInstance(sessionRoot, "session");
	}
	
	private static void addData(TreeElement root, String name, String data) {
		TreeElement datum = new TreeElement(name);
		datum.setValue(new UncastData(data));
		root.addChild(datum);
	}
	
	public EvaluationContext getEvaluationContext(Hashtable<String, DataInstance> instances) {
		
		FormInstance session = getSessionInstance();
		
		if(!instances.containsKey("casedb")) {
			instances.put("casedb", new ExternalDataInstance("jr://instance/casedb", "casedb"));
		}
		if(!instances.containsKey("session")) {
			instances.put("session", new ExternalDataInstance("jr://instance/session","session"));
		}
//		if(!instances.containsKey("ages")) {
//			instances.put("ages", new ExternalDataInstance("jr://instance/fixture/ages","ages"));
//		}
		
		InstanceInitializationFactory iif = getIif();

		for(Enumeration en = instances.keys(); en.hasMoreElements(); ) {
			String key = (String)en.nextElement(); 
			instances.get(key).initialize(iif, key);
		}

		
		return new EvaluationContext(new EvaluationContext(session), instances, session.getRoot().getRef());
	}
}