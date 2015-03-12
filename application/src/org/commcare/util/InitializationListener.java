/**
 *
 */
package org.commcare.util;

import org.javarosa.j2me.log.HandledThread;

/**
 * @author ctsims
 *
 */
public abstract class InitializationListener {

    HandledThread thread;

    public void setInitThread(HandledThread t) {
        this.thread = t;
    }

    public abstract void onSuccess();

    public abstract void onFailure();

}
