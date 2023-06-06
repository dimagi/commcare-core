package org.commcare.xml;

import org.commcare.suite.model.Entry;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.kxml2.io.KXmlParser;

import java.util.Hashtable;

public class ParseInstance  {
    // make its own class to be used by menu and entry
    // make static
    static void parseInstance(Hashtable<String, DataInstance> instances, KXmlParser parser) {
        String instanceId = parser.getAttributeValue(null, "id");
        String location = parser.getAttributeValue(null, "src");
        instances.put(instanceId, new ExternalDataInstance(location, instanceId));
    }
}
