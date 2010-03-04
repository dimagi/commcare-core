/**
 * 
 */
package org.commcare.reference;

import java.io.InputStream;

/**
 * @author ctsims
 *
 */
public interface Reference {
	public boolean doesBinaryExist();
	public InputStream getStream();
	public boolean isReadOnly();
	public String getRaw();
	public String getURI();
	public Reference contextualize(String raw);
}
