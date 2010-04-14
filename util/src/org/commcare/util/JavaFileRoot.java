/**
 * 
 */
package org.commcare.util;

import org.javarosa.core.reference.PrefixedRootFactory;
import org.javarosa.core.reference.Reference;


/**
 * @author ctsims
 *
 */
public class JavaFileRoot extends PrefixedRootFactory {

	String localRoot;
	
	public JavaFileRoot(String localRoot) {
		super(new String[] {"file"});
		this.localRoot = localRoot;
	}

	protected Reference factory(String terminal, String URI) {
		return new JavaFileReference(localRoot, terminal);
	}
}
