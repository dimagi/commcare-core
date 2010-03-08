/**
 * 
 */
package org.commcare.applogic;

import java.util.Enumeration;
import java.util.Hashtable;

import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareUtil;
import org.javarosa.cases.util.CaseEntity;
import org.javarosa.chsreferral.util.ReferralEntity;
import org.javarosa.core.api.State;
import org.javarosa.j2me.view.J2MEDisplay;

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
	public void entry(Entry entry) {
		CommCareUtil.launchEntry(entry, this);
	}
}
