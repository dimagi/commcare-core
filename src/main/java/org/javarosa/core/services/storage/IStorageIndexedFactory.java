package org.javarosa.core.services.storage;

public interface IStorageIndexedFactory {
    IStorageUtilityIndexed newStorage(String name, Class type);
}
