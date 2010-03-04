/**
 * 
 */
package org.commcare.resources.model;

/**
 * @author ctsims
 *
 */
public class UnresolvedResourceException extends Exception {
	Resource r;
	public UnresolvedResourceException(Resource r, String message) {
		super(message);
		this.r = r;
	}
	public Resource getResource() {
		return r;
	}
}
