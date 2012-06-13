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
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.entity.model.Entity;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.j2me.view.ProgressIndicator;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.utilities.media.MediaUtils;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

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
	
	//I hate this...
	private Hashtable<Integer, Suite> suiteTable = new Hashtable<Integer,Suite>();
	private Hashtable<Integer, Entry> entryTable = new Hashtable<Integer,Entry>();
	private Hashtable<Integer, Menu> menuTable = new Hashtable<Integer,Menu>();

	public CommCareSessionController(CommCareSession session, State currentState) {
		this.session = session;
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
					for(int i = 0; i < m.getCommandIds().size(); ++i) {
						String id = m.getCommandIds().elementAt(i);
						XPathExpression relevant = m.getRelevantCondition(i);
						if(relevant != null) {
							EvaluationContext ec  = session.getEvaluationContext(getIif());
							if(XPathFuncExpr.toBoolean(relevant.eval(ec)).booleanValue() == false) {
								continue;
							}
						}
						Entry e = suite.getEntries().get(id);
						int location = list.size();
						list.append(CommCareUtil.getEntryText(e,suite,location), MediaUtils.getImage(e.getImageURI()));
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
			
			//Start form entry and clear anything we've been using from memory
			initializer = null;
			
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
		
		SessionDatum datum = session.getNeededDatum();
		EvaluationContext context = session.getEvaluationContext(getIif());

		
		//TODO: This should be part of the next/back protocol in the session, not here.
		if(datum.getNodeset() == null) {
			//TODO: Generally this call makes a state happen, so this is going to fuck up going back.
			XPathExpression form;
			try {
				form = XPathParseTool.parseXPath(datum.getValue());
			} catch (XPathSyntaxException e) {
				//TODO: What.
				e.printStackTrace();
				throw new RuntimeException(e.getMessage());
			}
			if(datum.getType() == SessionDatum.DATUM_TYPE_FORM) {
				CommCareSessionController.this.session.setXmlns(XPathFuncExpr.toString(form.eval(context.getMainInstance(), context)));
				CommCareSessionController.this.session.setDatum("", "awful");
			} else {
				CommCareSessionController.this.session.setDatum(datum.getDataId(), XPathFuncExpr.toString(form.eval(context.getMainInstance(), context)));
			}
			next();
			return;
		}
		
		
		Detail shortDetail = suite.getDetail(datum.getShortDetail());
		Detail longDetail = null;
		if(datum.getLongDetail() != null) {
			longDetail = suite.getDetail(datum.getLongDetail());
		}
		
		final NodeEntitySet nes = new NodeEntitySet(datum.getNodeset(), context);
		Entity<TreeReference> entity = new CommCareEntity(shortDetail, longDetail, context, nes);
		
		final CommCareSelectState<TreeReference> select = new CommCareSelectState<TreeReference>(entity, nes) {
			SessionDatum datum;
			EvaluationContext context;

			{
				 datum = session.getNeededDatum();
				 context = session.getEvaluationContext(getIif());
			}
			
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
				String outputData = element.getValue().uncast().getString();
				CommCareSessionController.this.session.setDatum(datum.getDataId(), outputData);
				CommCareSessionController.this.next();
			}
		};
		
		J2MEDisplay.startStateWithLoadingScreen(select, new ProgressIndicator() {
			public double getProgress() {
				if(nes.loaded()) {
					return select.getProgressIndicator().getProgress();
				} else {
					return nes.getProgress();
				}
			}

			public String getCurrentLoadingStatus() {
				if(nes.loaded()) {
					return select.getProgressIndicator().getCurrentLoadingStatus();
				} else {
					return nes.getCurrentLoadingStatus();
				}
			}

			public int getIndicatorsProvided() {
				if(nes.loaded()) {
					return select.getProgressIndicator().getIndicatorsProvided();
				} else {
					return nes.getIndicatorsProvided();
				}
			}
			
		});
		return;
	}
	
	CommCareInstanceInitializer initializer = null;
	
	private InstanceInitializationFactory getIif() {
		if(initializer == null) {
			initializer = new CommCareInstanceInitializer(this);
		}
		return initializer;
	}

	private Vector<IPreloadHandler> getPreloaders() {
		return CommCareContext._().getPreloaders();
	}

	protected void back() {
		session.stepBack();
		next();
	}

	public FormInstance getSessionInstance() {
		return session.getSessionInstance(PropertyManager._().getSingularProperty(JavaRosaPropertyRules.DEVICE_ID_PROPERTY), 
                PropertyManager._().getSingularProperty(CommCareProperties.COMMCARE_VERSION),
                CommCareContext._().getUser().getUsername(),
                CommCareContext._().getUser().getUniqueId(),
                CommCareContext._().getUser().getProperties());
	}
}