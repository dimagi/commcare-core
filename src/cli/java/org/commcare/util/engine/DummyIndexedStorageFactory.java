package org.commcare.util.engine;

import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.externalizable.PrototypeFactory;

class DummyIndexedStorageFactory implements IStorageIndexedFactory {

    private PrototypeFactory prototypeFactory;

    public DummyIndexedStorageFactory(PrototypeFactory prototypeFactory) {
        this.prototypeFactory = prototypeFactory;
    }

    @Override
    public IStorageUtilityIndexed newStorage(String name, Class type) {
        return new DummyIndexedStorageUtility(type, prototypeFactory);
    }
}