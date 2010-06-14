/**
 * 
 */
package org.commcare.util;

/**
 * @author ctsims
 *
 */
public abstract class InitializationListener {
	
	Thread thread;

	public void setInitThread(Thread t) {
		this.thread = t;
	}

	public abstract void onSuccess();

	public abstract void onFailure();

}
