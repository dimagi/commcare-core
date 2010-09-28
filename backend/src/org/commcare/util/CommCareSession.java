/**
 * 
 */
package org.commcare.util;

import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;

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
	protected String currentCase;
	protected String currentRef;
	protected String currentRefType;
	protected String currentXmlns;
	
	/** CommCare needs a Command (an entry, view, etc) to proceed. Generally sitting on a menu screen. */
    public static final String STATE_COMMAND_ID = "COMMAND_ID";
    /** CommCare needs the ID of a menu to proceed. Generally called from the start state */
    public static final String STATE_MENU_ID = "MENU_ID";
    /** CommCare needs the ID of a Case to proceed **/
    public static final String STATE_CASE_ID = "CASE_ID";
    /** CommCare needs the ID of a Referral to proceed **/
    public static final String STATE_REFERRAL_ID = "REFERRAL_ID";
    /** CommCare needs the XMLNS of the form to be entered to proceed **/
    public static final String STATE_FORM_XMLNS = "FORM_XMLNS";
	
	public CommCareSession(CommCarePlatform platform) {
		this.platform = platform;
	}
	
	public Vector<Entry> getEntriesForCommand(String commandId) {
		Hashtable<String,Entry> map = platform.getMenuMap();
		Menu menu = null;
		Entry entry = null;
		top:
		for(Suite s : platform.getInstalledSuites()) {
			for(Menu m : s.getMenus()) {
				//We need to see if everything in this menu can be matched
				if(currentCmd.equals(m.getId())) {
					menu = m;
					break top;
				}
				
				if(s.getEntries().containsKey(currentCmd)) {
					entry = s.getEntries().get(currentCmd);
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
				entries.addElement(e);
			}
		}
		return entries;
	}
	
	public String getNeededData() {
		if(this.getCommand() == null) {
			return STATE_COMMAND_ID;
		}
		
		Vector<Entry> entries = getEntriesForCommand(this.getCommand());
		
		//We might need to select for a form if the selected entries don't specify one
		//but rather specify the need to choose one.
		if(currentXmlns == null && 
				!(entries.size() == 1 && entries.elementAt(0).getXFormNamespace() != null)) {
			boolean needXmlns = false;
			for(Entry e : entries) {
				if(!e.getReferences().containsKey("form")){
					// We can't grab a referral yet, since 
					// there is an entry which doesn't use one
					needXmlns = false;
					break;
				} else {
					needXmlns = true;
				}
			}
			if(needXmlns) {
				return STATE_FORM_XMLNS;
			}
		}
		
		//Referrals require cases as well, and if a referral is chosen a case
		//will be too, so we'll check for it first.
		if(currentRef == null) {
			boolean needRef = false;
			for(Entry e : entries) {
				if(!e.getReferences().containsKey("referral")){
					// We can't grab a referral yet, since 
					// there is an entry which doesn't use one
					needRef = false;
					break;
				} else {
					needRef = true;
				}
			}
			if(needRef) {
				return STATE_REFERRAL_ID;
			}
		}
		
		if(currentCase == null) {
			boolean needCase = false;
			for(Entry e : entries) {
				if(!e.getReferences().containsKey("case")){
					// We can't grab a case yet, since 
					// there is an entry which doesn't use one
					needCase = false;
					break;
				} else {
					needCase = true;
				}
			}
			if(needCase) {
				return STATE_CASE_ID;
			}
		}
		
		//the only other thing we can need is a form command. If there's still
		//more than one applicable entry, we need to keep going
		if(entries.size() > 1 || !entries.elementAt(0).getCommandId().equals(this.getCommand())) {
			return STATE_COMMAND_ID;
		} else {
			return null;
		}
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

	
	public void setCaseId(String caseId) {
		this.currentCase = caseId;
	}
	
	public void setCommand(String commandId) {
		this.currentCmd = commandId;
	}
	
	public void setReferral(String referralId, String refType) {
		this.currentRef = referralId;
		this.currentRefType = refType;
	}
	
	public String getReferralId() {
		return this.currentRef;
	}
	
	public String getReferralType() {
		return this.currentRefType;
	}
	
	public void setXmlns(String xmlns) {
		this.currentXmlns = xmlns;
	}
	
	public String getCaseId() {
		return this.currentCase;
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
		this.currentCase = null;
		this.currentCmd = null;
		this.currentRef = null;
		this.currentRefType = null;
		this.currentXmlns = null;
	}
}
