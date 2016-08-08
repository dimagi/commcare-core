package org.commcare.suite.model;

import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class UserRestore implements Persistable {
    @Override
    public void setID(int ID) {

    }

    @Override
    public int getID() {
        return 0;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {

    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {

    }
}
