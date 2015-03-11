package org.javarosa.core.services.storage;

import java.util.Hashtable;

public interface IMetaData {

    //for the indefinite future, no meta-data field can have a value of null

    String[] getMetaDataFields();

    Object getMetaData(String fieldName);

}
