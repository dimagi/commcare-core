/**
 *
 */
package org.commcare.modern.reference;

import org.javarosa.core.reference.PrefixedRootFactory;
import org.javarosa.core.reference.Reference;

/**
 * @author ctsims
 *
 */
public class JavaHttpRoot extends PrefixedRootFactory {

    public JavaHttpRoot() {
        super(new String[] {"http://", "https://"});
    }

    protected Reference factory(String terminal, String URI) {
        return new JavaHttpReference(URI);
    }

}
