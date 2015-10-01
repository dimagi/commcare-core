package org.commcare.api.persistence;

import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * Methods that mostly are used around the mocks that replicate stuff from
 * other projects.
 *
 * @author ctsims
 * @author wspride
 */
public class SqlSandboxUtils {
    public static UserSqlSandbox getStaticStorage(String username) {
        PrototypeFactory factory = new PrototypeFactory(new ClassNameHasher());
        return new UserSqlSandbox(username);
    }
}
