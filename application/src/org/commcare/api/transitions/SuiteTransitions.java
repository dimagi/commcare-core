/**
 * 
 */
package org.commcare.api.transitions;

import org.commcare.suite.model.Entry;

/**
 * @author ctsims
 *
 */
public interface SuiteTransitions {
	public void entry(Entry entry);
	public void exit();
}
