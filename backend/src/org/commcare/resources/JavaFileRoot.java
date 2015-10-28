/**
 *
 */
package org.commcare.resources;

import org.javarosa.core.reference.PrefixedRootFactory;
import org.javarosa.core.reference.Reference;


/**
 * @author ctsims
 *
 */
public class JavaFileRoot extends PrefixedRootFactory {

    String localRoot;
    String authority;

    public JavaFileRoot(String localRoot) {
        super(new String[] {"file"});
        this.localRoot = localRoot;
    }
    public JavaFileRoot(String[] uriRoots, String localRoot) {
        super(uriRoots);
        if(uriRoots.length ==1 ){
            authority = uriRoots[0];
        }
        this.localRoot = localRoot;
    }

    protected Reference factory(String terminal, String URI) {
        if(authority != null) {
            return new JavaFileReference(localRoot, terminal, authority);
        } else {
            return new JavaFileReference(localRoot, terminal);
        }
    }
}
