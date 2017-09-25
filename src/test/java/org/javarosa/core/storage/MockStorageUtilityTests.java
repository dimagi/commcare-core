package org.javarosa.core.storage;

import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;

/**
 * Tests for the mock object, important since it's used in various in-memory implementations
 *
 * Created by ctsims on 9/25/2017.
 */

public class MockStorageUtilityTests extends IndexedStorageUtilityTests{

    @Override
    protected IStorageUtilityIndexed<Shoe> createStorageUtility() {
        return new DummyIndexedStorageUtility<>(Shoe.class, new LivePrototypeFactory());
    }
}
