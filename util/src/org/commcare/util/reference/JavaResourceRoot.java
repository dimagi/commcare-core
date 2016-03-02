package org.commcare.util.reference;

import org.javarosa.core.reference.PrefixedRootFactory;
import org.javarosa.core.reference.Reference;

/**
 * Root factory for java classloader resources
 *
 * Created by ctsims on 8/14/2015.
 */
public class JavaResourceRoot  extends PrefixedRootFactory {

    final Class mHost;

    public JavaResourceRoot(Class host) {
        super(new String[]{"resource"});
        this.mHost = host;
    }

    protected Reference factory(String terminal, String URI) {
        return new JavaResourceReference(terminal, mHost);
    }
}
