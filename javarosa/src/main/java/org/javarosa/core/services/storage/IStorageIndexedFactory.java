package org.javarosa.core.services.storage;

public interface IStorageIndexedFactory extends IStorageFactory {
    IStorageUtilityIndexed newStorage(String name, Class type);
}
