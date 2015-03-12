/**
 *
 */
package org.commcare.api.transitions;

/**
 * @author ctsims
 *
 */
public interface FirstStartupTransitions {
    public void exit();
    public void login();
    public void restore();
}
