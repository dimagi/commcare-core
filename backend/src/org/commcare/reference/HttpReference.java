/**
 * 
 */
package org.commcare.reference;

import java.io.InputStream;

/**
 * @author ctsims
 *
 */
public class HttpReference implements Reference {

	String raw;
	String URI;
	
	public HttpReference(String raw, String URI) {
		this.raw = raw;
		this.URI = URI;
	}
	
	/* (non-Javadoc)
	 * @see org.commcare.reference.Reference#contextualize(java.lang.String)
	 */
	public Reference contextualize(String raw) {
		return new HttpReference(raw, ReferenceManager.contextualizeURI(this.URI, raw));
	}

	/* (non-Javadoc)
	 * @see org.commcare.reference.Reference#doesBinaryExist()
	 */
	public boolean doesBinaryExist() {
		//Do HTTP connection stuff? Look for a 404? 
		return true;
	}
	
	public InputStream getStream() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.commcare.reference.Reference#getRaw()
	 */
	public String getRaw() {
		return raw;
	}
	
	public String getURI() {
		return URI;
	}

	/* (non-Javadoc)
	 * @see org.commcare.reference.Reference#isReadOnly()
	 */
	public boolean isReadOnly() {
		return true;
	}
	
	public boolean equals(Object o) {
		if(o instanceof ResourceReference) {
			return URI.equals(((ResourceReference)o).URI);
		} else {
			return false;
		}
	}
}
