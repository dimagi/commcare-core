package org.commcare.modern.reference;

import org.javarosa.core.reference.PrefixedRootFactory;
import org.javarosa.core.reference.Reference;


/**
 * @author ctsims
 */
public class JavaFileRoot extends PrefixedRootFactory {

    final String localRoot;

    public JavaFileRoot(String localRoot) {
        super(new String[]{"file"});

        this.localRoot = localRoot;
    }

    @Override
    protected Reference factory(String terminal, String URI) {
        return new JavaFileReference(localRoot, terminal);
    }
}
