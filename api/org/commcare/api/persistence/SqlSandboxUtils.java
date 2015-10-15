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
        return new UserSqlSandbox(username);
    }
}
