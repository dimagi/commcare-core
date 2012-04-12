/**
 * 
 */
package org.commcare.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.Suite;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.util.OrderedHashtable;

/**
 * Before arriving at the Form Entry phase, CommCare applications
 * need to determine what form to enter, and with what pre-requisites.
 * 
 * A CommCare Session helps to encapsulate this information by identifying
 * the set of possible entry operations (Every piece of data needed to begin
 * entry) and specifying the operation which would most quickly filter our
 * the set of operations. 
 * 
 * NOTE: Currently horribly coupled to the platform.
 * 
 * @author ctsims
 *
 */
public class CommCareSession {

	public static final String ENTITY_NONE = "NONE";
	
	CommCarePlatform platform;
	
	protected String currentCmd;
	protected OrderedHashtable data;
	protected String currentXmlns;
	
	protected Vector<String[]> steps = new Vector<String[]>();
	
	protected String[] popped;
	
	/** CommCare needs a Command (an entry, view, etc) to proceed. Generally sitting on a menu screen. */
    public static final String STATE_COMMAND_ID = "COMMAND_ID";
    /** CommCare needs the ID of a Case to proceed **/
    public static final String STATE_DATUM_VAL = "CASE_ID";
    /** Computed Value **/
    public static final String STATE_DATUM_COMPUTED = "COMPTUED_DATUM";
    /** CommCare needs the XMLNS of the form to be entered to proceed **/
    public static final String STATE_FORM_XMLNS = "FORM_XMLNS";
	
	public CommCareSession(CommCarePlatform platform) {
		this.platform = platform;
		data = new OrderedHashtable();
	}

	public Vector<Entry> getEntriesForCommand(String commandId) {
		return this.getEntriesForCommand(commandId, new OrderedHashtable());
	}
	public Vector<Entry> getEntriesForCommand(String commandId, OrderedHashtable data) {
		Hashtable<String,Entry> map = platform.getMenuMap();
		Menu menu = null;
		Entry entry = null;
		top:
		for(Suite s : platform.getInstalledSuites()) {
			for(Menu m : s.getMenus()) {
				//We need to see if everything in this menu can be matched
				if(commandId.equals(m.getId())) {
					menu = m;
					break top;
				}
				
				if(s.getEntries().containsKey(commandId)) {
					entry = s.getEntries().get(commandId);
					break top;
				}
			}
		}
		
		Vector<Entry> entries = new Vector<Entry>();
		if(entry != null) {
			entries.addElement(entry);
		}
		
		if(menu != null) {
			//We're in a menu we have a set of requirements which
			//need to be fulfilled
			for(String cmd : menu.getCommandIds()) {
				Entry e = map.get(cmd);
				if(e == null) { throw new RuntimeException("No entry found for menu command [" + cmd + "]"); }
				boolean valid = true;
				Vector<SessionDatum> requirements = e.getSessionDataReqs();
				if(requirements.size() >= data.size()) {
					for(int i = 0 ; i < data.size() ; ++i ) {
						if(!requirements.elementAt(i).getDataId().equals(data.keyAt(i)))  {
							valid = false;
						}
					}
				}
				if(valid) {
					entries.addElement(e);
				}
			}
		}
		return entries;
	}
	
	protected OrderedHashtable getData() {
		return data;
	}
	
	public String getNeededData() {
		if(this.getCommand() == null) {
			return STATE_COMMAND_ID;
		}
		
		Vector<Entry> entries = getEntriesForCommand(this.getCommand(), this.getData());
		
		//Get data. Checking first to see if the relevant key is needed by all entries
		
		String needDatum = null;
		String nextKey = null;
		for(Entry e : entries) {
			if(e.getSessionDataReqs().size() > this.getData().size()) {
				SessionDatum datum = e.getSessionDataReqs().elementAt(this.getData().size());
				String needed = datum.getDataId();
				if(nextKey == null) {
					nextKey = needed;
					if(datum.getNodeset() != null) {
						needDatum = STATE_DATUM_VAL;
					} else {
						needDatum = STATE_DATUM_COMPUTED;
					}
					continue;
				} else {
					//TODO: Detail screen matchup seems relevant? Maybe?
					if(nextKey.equals(needed)) {
						continue;
					}
				}
			}
			
			//If we made it here, we either don't need more data or don't need
			//consistent data for the remaining options
			needDatum = null;
			break;
		}
		if(needDatum != null) {
			return needDatum;
		}
		
		//the only other thing we can need is a form command. If there's still
		//more than one applicable entry, we need to keep going
		if(entries.size() > 1 || !entries.elementAt(0).getCommandId().equals(this.getCommand())) {
			return STATE_COMMAND_ID;
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public SessionDatum getNeededDatum() {
		Entry entry = getEntriesForCommand(getCommand()).elementAt(0);		
		SessionDatum datum = entry.getSessionDataReqs().elementAt(getData().size());
		return datum;
	}

	public Detail getDetail(String id) {
		for(Suite s : platform.getInstalledSuites()) {
			Detail d = s.getDetail(id);
			if(d != null) {
				return d;
			}
		}
		return null;
	}
	
	public Menu getMenu(String id) {
		for(Suite suite : platform.getInstalledSuites()) {
			for(Menu m : suite.getMenus()) {
				if(id.equals(m.getId())) {
					return m;
				}
			}
		}
		return null;
	}
	
	public Suite getCurrentSuite() {
		for(Suite s : platform.getInstalledSuites()) {
			for(Menu m : s.getMenus()) {
				//We need to see if everything in this menu can be matched
				if(currentCmd.equals(m.getId())) {
					return s;
				}
				
				if(s.getEntries().containsKey(currentCmd)) {
					return s;
				}
			}
		}
		
		return null;
	}
	
	public void stepBack() {
		String[] recentPop = null;
		if(steps.size() > 0) {
			recentPop = steps.elementAt(steps.size() -1);
			steps.removeElementAt(steps.size() - 1);
		}
		syncState();
		popped = recentPop;
		//If we've stepped back into a computed value, we actually want to go back again, since evaluating that
		//element will just result in moving forward again.
		if(this.getNeededData() == STATE_DATUM_COMPUTED) {
			stepBack();
		}
	}

	public void setDatum(String keyId, String value) {
		this.steps.addElement(new String[] {STATE_DATUM_VAL, keyId, value});
		syncState();
	}
	
	public void setXmlns(String xmlns) {
		this.steps.addElement(new String[] {STATE_FORM_XMLNS, xmlns});
		syncState();
	}
	
	public void setCommand(String commandId) {
		this.steps.addElement(new String[] {STATE_COMMAND_ID, commandId});
		syncState();
	}
	
	private void syncState() {
		this.data.clear();
		this.currentCmd = null;
		this.currentXmlns = null;
		this.popped = null;
		
		for(String[] step : steps) {
			if(STATE_DATUM_VAL.equals(step[0])) {
				String key = step[1];
				String value = step[2];
				data.put(key, value);
			} else if(STATE_COMMAND_ID.equals(step[0])) {
				this.currentCmd = step[1];
			}  else if(STATE_FORM_XMLNS.equals(step[0])) {
				this.currentXmlns = step[1];
			}
		}
	}
	
	public String[] getPoppedStep() {
		return popped;
	}
	
	public String getForm() {
		if(this.currentXmlns != null) { 
			return this.currentXmlns;
		} 
		String command = getCommand();
		if(command == null) { return null; }
		
		Entry e = platform.getMenuMap().get(command);
		return e.getXFormNamespace();
	}
	
	public String getCommand() {
		return this.currentCmd;
	}
	
	public void clearState() {
		steps.removeAllElements();
		syncState();
	}
	
	public FormInstance getSessionInstance(String deviceId, String appversion, String username, String userId) {
		TreeElement sessionRoot = new TreeElement("session",0);
		
		TreeElement sessionData = new TreeElement("data",0);
		
		sessionRoot.addChild(sessionData);
		
		for(String[] step : steps) {
			if(step[0] == CommCareSession.STATE_DATUM_VAL) {
				TreeElement datum = new TreeElement(step[1]);
				datum.setValue(new UncastData(step[2]));
				sessionData.addChild(datum);
			}
		}
		
		TreeElement sessionMeta = new TreeElement("context",0);

		addData(sessionMeta, "deviceid", deviceId);
		addData(sessionMeta, "appversion", appversion);
		addData(sessionMeta, "username", username);
		addData(sessionMeta, "userid",userId );

		sessionRoot.addChild(sessionMeta);
		
		return new FormInstance(sessionRoot, "session");
	}
	
	private static void addData(TreeElement root, String name, String data) {
		TreeElement datum = new TreeElement(name);
		datum.setValue(new UncastData(data));
		root.addChild(datum);
	}
	
	
	public EvaluationContext getEvaluationContext(InstanceInitializationFactory iif) {
		
		Entry entry = getEntriesForCommand(getCommand()).elementAt(0);
		
		Hashtable<String, DataInstance> instances = entry.getInstances();

		for(Enumeration en = instances.keys(); en.hasMoreElements(); ) {
			String key = (String)en.nextElement(); 
			instances.get(key).initialize(iif, key);
		}

		
		return new EvaluationContext(null, instances);
	}

	public Vector<String[]> getSteps() {
		//TODO: Type safe copy
		return steps;
	}
}
