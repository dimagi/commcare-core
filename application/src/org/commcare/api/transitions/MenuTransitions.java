/**
 *
 */
package org.commcare.api.transitions;

import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Suite;

/**
 * @author ctsims
 *
 */
public interface MenuTransitions {
    public void entry(Suite suite, Entry entry);
    public void exitMenuTransition();
}
