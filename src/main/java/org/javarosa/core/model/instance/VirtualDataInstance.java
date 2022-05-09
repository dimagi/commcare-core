package org.javarosa.core.model.instance;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Data instance that is created during execution based on user input of other live data
 */
public class VirtualDataInstance extends ExternalDataInstance {

    public VirtualDataInstance() {
    }

    public VirtualDataInstance(String reference, String instanceid) {
        super(reference, instanceid);
    }

    /**
     * Copy constructor
     */
    public VirtualDataInstance(VirtualDataInstance instance) {
        super(instance);
    }

    public VirtualDataInstance(String reference, String instanceId, TreeElement topLevel) {
        super(reference, instanceId, topLevel, null);
    }
}
