/**
 * 
 */
package org.commcare.util;

import java.util.Vector;

/**
 * @author ctsims
 *
 */
public class SessionFrame {	
	/** CommCare needs a Command (an entry, view, etc) to proceed. Generally sitting on a menu screen. */
    public static final String STATE_COMMAND_ID = "COMMAND_ID";
    /** CommCare needs the ID of a Case to proceed **/
    public static final String STATE_DATUM_VAL = "CASE_ID";
    /** Computed Value **/
    public static final String STATE_DATUM_COMPUTED = "COMPTUED_DATUM";
    /** CommCare needs the XMLNS of the form to be entered to proceed **/
    public static final String STATE_FORM_XMLNS = "FORM_XMLNS";
	
	public static final String ENTITY_NONE = "NONE";
	
	private String frameId;
	protected Vector<String[]> steps = new Vector<String[]>();
	
	/**
	 * Create a new, un-id'd session frame
	 */
	public SessionFrame() {
		
	}

	
	public SessionFrame(String frameId) {
		this.frameId = frameId;
	}



	public Vector<String[]>  getSteps() {
		return steps;
	}



	public String[] popStep() {
		String[] recentPop = null;
		
		if(steps.size() > 0) {
			recentPop = steps.elementAt(steps.size() -1);
			steps.removeElementAt(steps.size() - 1);
		}
		return recentPop;
	}



	public void pushStep(String[] step) {
		steps.addElement(step);
	}



	public String getFrameId() {
		return frameId;
	}

}
