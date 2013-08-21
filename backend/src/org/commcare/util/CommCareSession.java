/**
 * 
 */
package org.commcare.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.suite.model.StackOperation;
import org.commcare.suite.model.Suite;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.locale.Localizer;
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
	
	CommCarePlatform platform;
		
	protected String[] popped;
	
	protected String currentCmd;
	protected OrderedHashtable data;
	protected String currentXmlns;
	
	/** The current session frame data **/
	SessionFrame frame;
	/** The stack of pending Frames **/
	Stack<SessionFrame> frameStack;
	
	public CommCareSession(CommCarePlatform platform) {
		this.platform = platform;
		data = new OrderedHashtable();
		this.frame = new SessionFrame();
		this.frameStack = new Stack<SessionFrame>();
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
			return SessionFrame.STATE_COMMAND_ID;
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
						needDatum = SessionFrame.STATE_DATUM_VAL;
					} else {
						needDatum = SessionFrame.STATE_DATUM_COMPUTED;
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
			return SessionFrame.STATE_COMMAND_ID;
		} else {
			return null;
		}
	}

	public String[] getHeaderTitles() {
		Hashtable<String, String> menus = new Hashtable<String, String>();
		
		for(Suite s : platform.getInstalledSuites()) {
			for(Menu m : s.getMenus()) {
				menus.put(m.getId(), m.getName().evaluate());
			}
		}
		
		Vector<String[]> steps = frame.getSteps();
		String[] returnVal = new String[steps.size()];
		
		
		Hashtable<String, Entry> entries = platform.getMenuMap();
		int i = 0;
		for(String[] step : steps) {
			if(step[0] == SessionFrame.STATE_COMMAND_ID) {
				//Menu or form. 
				if(menus.containsKey(step[1])) {
					returnVal[i] = menus.get(step[1]);
				} else if(entries.containsKey(step[1])) {
					returnVal[i] = entries.get(step[1]).getText().evaluate();
				}
			} else if(step[0] == SessionFrame.STATE_DATUM_VAL) {
				//TODO: Grab the name of the case
			}  else if(step[0] == SessionFrame.STATE_DATUM_COMPUTED) {
				//Nothing to do here
			}
			
			if(returnVal[i] != null) {
				//Menus contain a potential argument listing where that value is on the screen, 
				//clear it out if it exists
				returnVal[i] = Localizer.processArguments(returnVal[i], new String[] {""}).trim();
			}
			
			++i;
		}
		
		return returnVal;
	}
	
	/**
	 * @return The next relevant datum for the current entry. Requires there to be
	 * an entry on the stack
	 */
	public SessionDatum getNeededDatum() {
		Entry entry = getEntriesForCommand(getCommand()).elementAt(0);		
		return getNeededDatum(entry);
	}
	
	/**
	 * @param entry An entry which is consistent as a step on the stack
	 * @return A session datum definition if one is pending. Null otherwise.
	 */
	public SessionDatum getNeededDatum(Entry entry) {
		int nextVal = getData().size();
		//If we've already retrieved all data needed, return null.
		if(nextVal >= entry.getSessionDataReqs().size()) { return null; }
		
		//Otherwise retrieve the needed value
		SessionDatum datum = entry.getSessionDataReqs().elementAt(nextVal);
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
		String[] recentPop = frame.popStep();
		//TODO: Check the "base state" of the frame
		//after popping to see if we invalidated the
		//stack

		syncState();
		popped = recentPop;
		//If we've stepped back into a computed value, we actually want to go back again, since evaluating that
		//element will just result in moving forward again.
		if(this.getNeededData() == SessionFrame.STATE_DATUM_COMPUTED) {
			stepBack();
		}
	}

	public void setDatum(String keyId, String value) {
		frame.pushStep(new String[] {SessionFrame.STATE_DATUM_VAL, keyId, value});
		syncState();
	}
	
	public void setXmlns(String xmlns) {
		frame.pushStep(new String[] {SessionFrame.STATE_FORM_XMLNS, xmlns});
		syncState();
	}
	
	public void setCommand(String commandId) {
		frame.pushStep(new String[] {SessionFrame.STATE_COMMAND_ID, commandId});
		syncState();
	}
	
	private void syncState() {
		this.data.clear();
		this.currentCmd = null;
		this.currentXmlns = null;
		this.popped = null;
		
		for(String[] step : frame.getSteps()) {
			if(SessionFrame.STATE_DATUM_VAL.equals(step[0])) {
				String key = step[1];
				String value = step[2];
				data.put(key, value);
			} else if(SessionFrame.STATE_COMMAND_ID.equals(step[0])) {
				this.currentCmd = step[1];
			}  else if(SessionFrame.STATE_FORM_XMLNS.equals(step[0])) {
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
	
	/**
	 * Clear the current stack frame and release any pending
	 * stack frames (completely clearing the session)
	 */
	public void clearAllState() {
		frame = new SessionFrame();
		frameStack.removeAllElements();
		syncState();
	}
	
	public FormInstance getSessionInstance(String deviceId, String appversion, String username, String userId, Hashtable<String, String> userFields) {
		TreeElement sessionRoot = new TreeElement("session",0);
		
		TreeElement sessionData = new TreeElement("data",0);
		
		sessionRoot.addChild(sessionData);
		
		for(String[] step : frame.getSteps()) {
			if(step[0] == SessionFrame.STATE_DATUM_VAL) {
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
		
		TreeElement user = new TreeElement("user",0);
		TreeElement userData = new TreeElement("data",0);
		user.addChild(userData);
		for(Enumeration en = userFields.keys() ; en.hasMoreElements();) {
			String key = (String)en.nextElement();
			addData(userData, key, userFields.get(key));
		}
		
		sessionRoot.addChild(user);
		
		return new FormInstance(sessionRoot, "session");
	}
	
	private static void addData(TreeElement root, String name, String data) {
		TreeElement datum = new TreeElement(name);
		datum.setValue(new UncastData(data));
		root.addChild(datum);
	}
	
	
	public EvaluationContext getEvaluationContext(InstanceInitializationFactory iif) {
		
		if(getCommand() == null) { return new EvaluationContext(null); } 
		Entry entry = getEntriesForCommand(getCommand()).elementAt(0);
		
		Hashtable<String, DataInstance> instances = entry.getInstances();

		for(Enumeration en = instances.keys(); en.hasMoreElements(); ) {
			String key = (String)en.nextElement(); 
			instances.get(key).initialize(iif, key);
		}

		
		return new EvaluationContext(null, instances);
	}

	public SessionFrame getFrame() {
		//TODO: Type safe copy
		return frame;
	}
	
	public void executeStackOperation(StackOperation op, EvaluationContext ec) {
		//First, see if there is a frame with a matching ID (relevant for a couple
		//reasons, and possibly prevents a costly XPath lookup
		String frameId = op.getFrameId();
		SessionFrame matchingFrame = null;
		if(frameId != null) {
			//TODO: This is correct, right? We want to treat the current frame
			//as part of the "environment" and not let people create a new frame
			//with the same id?
			if(frameId.equals(frame.getFrameId())) {
				matchingFrame = frame;
			} else {
				//Otherwise, peruse the stack looking for another
				//frame with a matching ID.
				for(Enumeration e = frameStack.elements() ; e .hasMoreElements() ;) {
					SessionFrame stackFrame = (SessionFrame)e.nextElement();
					if(frameId.equals(stackFrame.getFrameId())) {
						matchingFrame = stackFrame;
						break;
					}
				}
			}
		}
		
		boolean newFrame = false;
		switch(op.getOp()) {
		//Note: the Create step and Push step utilize the same code, 
		//and the create step does some setup first
		case StackOperation.OPERATION_CREATE:
			//First make sure we have no existing frames with this ID
			if(matchingFrame != null) {
				//If we do, just bail.
				return;
			}
			//Otherwise, create our new frame (we'll only manipulate it
			//and add it if it is triggered)
			matchingFrame = new SessionFrame(frameId);
			
			//Ok, now fall through to the push case using that frame, 
			//as the push operations are ~identical
			newFrame = true;
		case StackOperation.OPERATION_PUSH:
			
			//Ok, first, see if we need to execute this op
			if(!op.isOperationTriggered(ec)){
				//Otherwise, we're done.
				return;
			}
			
			//If this is a fresh push, grab the frame, if this is
			//a new push 
			if(matchingFrame == null) { matchingFrame = frame;}
			
			//Now, execute the steps in this operation
			for(StackFrameStep step : op.getStackFrameSteps()) {
				matchingFrame.pushStep(step.defineStep(ec));
			}
			
			//ok, frame should be appropriately modified now. 
			//we also need to push this frame if it's new 
			if(newFrame){
				frameStack.push(matchingFrame);
			}
			break;
		case StackOperation.OPERATION_CLEAR:
			if(matchingFrame != null) {
				if(op.isOperationTriggered(ec)) {
					frameStack.removeElement(matchingFrame);
				}
			}
			break;
		default:
			throw new RuntimeException("Undefined stack operation: " + op.getOp());
		}
		
		//Not sure if we should sync here, probably not, only the push op could result in a sync,
		//so we should take care of it there.
	}

	/**
	 * Complete the current session (and perform any cleanup), then
	 * check the stack for any pending frames, and load the top one
	 * into the current session if so. 
	 * 
	 * @return True if there was a pending frame and it has been
	 * popped into the current session. False if the stack was empty
	 * and the session is over.
	 */
	public boolean finishAndPop() {
		if(frameStack.empty()) {
			return false;
		} else {
			frame = frameStack.pop();
			syncState();
			return true;
		}
	}

	public Entry getCurrentEntry() {
		Vector<Entry> e = getEntriesForCommand(getCommand());
		if(e.size() > 1) {
			throw new IllegalStateException("The current session does not contain a single valid entry");
		} if(e.size() == 0){
			throw new IllegalStateException("The current session has no valid entry");
		}
		return e.elementAt(0);
	}
}
