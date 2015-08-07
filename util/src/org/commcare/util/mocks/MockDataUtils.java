package org.commcare.util.mocks;

import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * Methods that mostly are used around the mocks that replicate stuff from
 * other projects.
 *
 * TODO: We should try to centralize how these are used.
 *
 * @author ctsims
 */
public class MockDataUtils {

    public static MockUserDataSandbox getStaticStorage() {
        PrototypeFactory factory = new PrototypeFactory(new ClassNameHasher());
        return new MockUserDataSandbox(factory);
    }
}
