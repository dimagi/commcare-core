package org.javarosa.core.model.condition;

/**
 * An object which can be passed to convey
 *
 * Created by ctsims on 9/28/2017.
 */

public class LifecycleSignaler implements Abandonable {
    private boolean isAbandoned = false;

    /**
     * If the current lifecycle has been abandoned, this will throw a
     * RequestAbandonedException in order to terminate the current process
     */
    @Override
    public void assertNotAbandoned() {
        if(isAbandoned) {
            throw new RequestAbandonedException();
        }
    }

    @Override
    public void signalAbandoned() {
        this.isAbandoned = true;
    }

    public static void AssertNotAbandoned(Abandonable abandonable) {
        if(abandonable == null) {
            return;
        }
        abandonable.assertNotAbandoned();
    }

    public static void SignalAbandoned(Abandonable abandonable) {
        if(abandonable == null) {
            return;
        }
        abandonable.signalAbandoned();
    }
}
