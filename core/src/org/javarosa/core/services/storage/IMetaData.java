package org.javarosa.core.services.storage;

public interface IMetaData {

    //for the indefinite future, no meta-data field can have a value of null

    String[] getMetaDataFields();

    Object getMetaData(String fieldName);
}
