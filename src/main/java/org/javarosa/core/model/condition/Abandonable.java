package org.javarosa.core.model.condition;

/**
 * An implementing object can be signaled as "abandoned", which means the following:
 *
 * 1) The object is no longer useful, and should cease all execution immediately if possible
 * 2) It should be acceptable for any future request or use of this object to result in an
 * exception
 * 3) The object should free any/all memory it is accountable for if it won't be able to halt any
 * pending execution immediately
 * 4) If any dependent objects are abandonable, the signal should be forwarded along to them.
 *
 * Created by ctsims on 9/28/2017.
 */

public interface Abandonable {

    /**
     * If the current lifecycle has been abandoned, this will throw a
     * RequestAbandonedException in order to terminate the current process
     */
    void assertNotAbandoned();

    /**
     * Signal that the object and context are now abandoned.
     *
     * This is expected to be called from a thread other than the one where work is taking
     * place currently. If there is cleanup work to be done that would take an appreciable
     * amount of time, it should ideally take place _outside_ of this call, but rather when
     * the signal is detected on the other thread
     */
    void signalAbandoned();
}
