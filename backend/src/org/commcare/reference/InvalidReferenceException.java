/**
 * 
 */
package org.commcare.reference;

/**
 * @author ctsims
 *
 */
public class InvalidReferenceException extends Exception {
	private String reference;
	public InvalidReferenceException(String message, String referece) {
		super(message);
		this.reference = reference;
	}
	public String getReferenceString() {
		return reference;
	}
}
