/**
 * 
 */
package org.commcare.resources.model;

/**
 * An UnreliableSourceException is a special type of UnresolvedResourceException which signals
 * that a resource was not available when the attmept was made to resolve it, but that resource
 * may be available in the future, due to potentially lossy channels like HTTP/bluetooth, etc.
 * 
 * This exception should only be caught by name (compared to a URE) when an attempt will be
 * made to retry the resource resolution.
 * 
 * @author ctsims
 *
 */
public class UnreliableSourceException extends UnresolvedResourceException {
	private boolean shouldBreak=false;
	public UnreliableSourceException(Resource r, String message) {
		super(r, message);
	}
	public UnreliableSourceException(Resource r, String message, boolean shouldBreak){
		super(r,message);
		this.shouldBreak = shouldBreak;
	}
	public boolean shouldBreak(){
		return shouldBreak;
	}
}
