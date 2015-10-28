/**
 *
 */
package org.commcare.api.engine.models;

import java.util.Vector;

/**
 * @author ctsims
 *
 */
public class Step {
    Action action;
    Vector<Assertion> assertions;

    public Step() {

    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void addAssertion(Assertion assertion) {
        assertions.add(assertion);
    }

    public Action getAction() {
        return action;
    }
}
