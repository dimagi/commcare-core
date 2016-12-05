package org.javarosa.core.services;

import org.javarosa.core.services.properties.IPropertyRules;

import java.util.Vector;

/**
 * An IProperty Manager is responsible for setting and retrieving name/value pairs
 *
 * @author Yaw Anokwa
 */
public interface IPropertyManager {

    Vector getProperty(String propertyName);

    void setProperty(String propertyName, String propertyValue);

    void setProperty(String propertyName, Vector<String> propertyValue);

    String getSingularProperty(String propertyName);

    void addRules(IPropertyRules rules);

    Vector getRules();
}
