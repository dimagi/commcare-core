/**
 * 
 */
package org.commcare.applogic;

import java.util.Enumeration;
import java.util.Hashtable;

import org.commcare.suite.model.Entry;
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

	public CommCareSuiteHomeState(Suite suite) {
		super(suite);
	}
	
	/* (non-Javadoc)
	 * @see org.commcare.api.transitions.SuiteTransitions#entry(org.commcare.suite.model.Entry)
	 */
	public void entry(Entry entry) {
		final Entry e = entry;
		
		Hashtable<String, String> references = entry.getReferences();
		if(references.size() == 0) {
			String namespace = entry.getXFormNamespace();
			CommCareFormEntryState state = new CommCareFormEntryState(namespace, CommCareContext._().getPreloaders(), CommCareContext._().getFuncHandlers()) {
				protected void goHome() {
					J2MEDisplay.startStateWithLoadingScreen(CommCareSuiteHomeState.this);			
				}
			};
			J2MEDisplay.startStateWithLoadingScreen(state);
		}
		else {
			//this will be revisited and rewritten 
			boolean referral = false;
			// Need to do some reference gathering... 
			for(Enumeration en = references.keys() ; en.hasMoreElements() ; ) {
				String key = (String)en.nextElement();
				String refType = references.get(key);
				if(refType.toLowerCase().equals("referral")) {
					referral = true;
				}
			}
			State select = null;
			if(referral) {
				select = new CommCareReferralSelectState(new ReferralEntity()) {
					
					public void cancel() {
						J2MEDisplay.startStateWithLoadingScreen(CommCareSuiteHomeState.this);
					}
					
					public void entitySelected(int id) {
						CommCareFormEntryState state = new CommCareFormEntryState(e.getXFormNamespace(), CommCareContext._().getPreloaders(CommCareUtil.getReferral(id)), CommCareContext._().getFuncHandlers()) {
							protected void goHome() {
								J2MEDisplay.startStateWithLoadingScreen(CommCareSuiteHomeState.this);
							}
						};
						J2MEDisplay.startStateWithLoadingScreen(state);
					}
				};
			} else {
				select = new CommCareCaseSelectState(new CaseEntity()) {
					
					public void cancel() {
						J2MEDisplay.startStateWithLoadingScreen(CommCareSuiteHomeState.this);
					}
					
					public void entitySelected(int id) {
						String form = e.getXFormNamespace();
						State state;
						if(form == null) {
							state = CommCareSuiteHomeState.this;
						} else {
							state = new CommCareFormEntryState(form, CommCareContext._().getPreloaders(CommCareUtil.getCase(id)), CommCareContext._().getFuncHandlers()) {
								protected void goHome() {
									J2MEDisplay.startStateWithLoadingScreen(CommCareSuiteHomeState.this);					
								}
							};
						}
						J2MEDisplay.startStateWithLoadingScreen(state);
					}
				};
			}
			J2MEDisplay.startStateWithLoadingScreen(select);
		}
	}
}
