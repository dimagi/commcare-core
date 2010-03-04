/**
 * 
 */
package org.commcare.reference;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author ctsims
 *
 */
public class ResourceReference implements Reference {
	
	String raw;
	String URI;
	
	public ResourceReference(String raw, String URI) {
		this.raw = raw;
		this.URI = URI;
	}
	
	public boolean doesBinaryExist() {
		InputStream is = System.class.getResourceAsStream(URI);
		if(is == null) {
			return false;
		} else {
			try {
				is.close();
			} catch(IOException e) {
				//TODO: Honestly, I dunno what to do about this, it happens
				//sometimes...
				e.printStackTrace();
			}
			return true;
		}
	}
	
	public InputStream getStream() {
		InputStream is = System.class.getResourceAsStream(URI);
		return is;
	}

	public String getRaw() {
		return raw;
	}
	
	public String getURI() {
		return "jr:/" + this.URI;
	}
	
	public Reference contextualize(String raw) {
		return new ResourceReference(raw, ReferenceUtil.contextualizeURI(this.URI, raw));
	}

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
