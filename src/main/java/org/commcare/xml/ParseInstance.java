package org.commcare.xml;

import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.kxml2.io.KXmlParser;

import java.util.Hashtable;

public class ParseInstance {
    static void parseInstance(Hashtable<String, DataInstance> instances, KXmlParser parser) {
        String instanceId = parser.getAttributeValue(null, "id");
        String location = parser.getAttributeValue(null, "src");
        instances.put(instanceId, new ExternalDataInstance(location, instanceId));
    }
}
