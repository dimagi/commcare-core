/**
 * 
 */
package org.commcare.applogic;

import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareUtil;

/**
 * @author ctsims
 *
 */
public abstract class CommCareSuiteHomeState extends SuiteHomeState {

	public CommCareSuiteHomeState(Suite suite, Menu m) {
		super(suite, m);
	}
	
	/* (non-Javadoc)
	 * @see org.commcare.api.transitions.SuiteTransitions#entry(org.commcare.suite.model.Entry)
	 */
	public void entry(Suite suite, Entry entry) {
		CommCareUtil.launchEntry(suite, entry, this);
	}
}
