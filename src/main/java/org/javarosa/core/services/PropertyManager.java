package org.javarosa.core.services;

import org.javarosa.core.services.properties.IPropertyRules;
import org.javarosa.core.services.properties.Property;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * PropertyManager is a class that is used to set and retrieve name/value pairs
 * from persistent storage.
 *
 * Which properties are allowed, and what they can be set to, can be specified by an implementation of
 * the IPropertyRules interface, any number of which can be registered with a property manager. All
 * property rules are inclusive, and can only increase the number of potential properties or property
 * values.
 *
 * @author Clayton Sims
 */
public class PropertyManager implements IPropertyManager {

    /**
     * The name for the Persistent storage utility name
     */
    public static final String STORAGE_KEY = "PROPERTY";

    /**
     * The persistent storage utility
     */
    private final IStorageUtilityIndexed properties;

    /**
     * Constructor for this PropertyManager
     */
    public PropertyManager(IStorageUtilityIndexed properties) {
        this.properties = properties;
    }

    /**
     * Retrieves the singular property specified, as long as it exists in one of the current rulesets
     *
     * @param propertyName the name of the property being retrieved
     * @return The String value of the property specified if it exists, is singluar, and is in one the current
     * rulessets. null if the property is denied by the current ruleset, or is a vector.
     */
    @Override
    public String getSingularProperty(String propertyName) {
        String retVal = null;
        Vector value = getValue(propertyName);
        if (value != null && value.size() == 1) {
            retVal = (String)value.elementAt(0);
        }
        return retVal;
    }


    /**
     * Retrieves the property specified, as long as it exists in one of the current rulesets
     *
     * @param propertyName the name of the property being retrieved
     * @return The String value of the property specified if it exists, and is the current ruleset, if one exists.
     * null if the property is denied by the current ruleset.
     */
    @Override
    public Vector getProperty(String propertyName) {
        return getValue(propertyName);
    }

    /**
     * Sets the given property to the given string value, if both are allowed by any existing ruleset
     *
     * @param propertyName  The property to be set
     * @param propertyValue The value that the property will be set to
     */
    @Override
    public void setProperty(String propertyName, String propertyValue) {
        Vector<String> wrapper = new Vector<>();
        wrapper.addElement(propertyValue);
        setProperty(propertyName, wrapper);
    }

    /**
     * Sets the given property to the given vector value, if both are allowed by any existing ruleset
     *
     * @param propertyName  The property to be set
     * @param propertyValue The value that the property will be set to
     */
    @Override
    public void setProperty(String propertyName, Vector<String> propertyValue) {
        Vector oldValue = getProperty(propertyName);
        if (oldValue != null && vectorEquals(oldValue, propertyValue)) {
            //No point in redundantly setting values!
            return;
        }
        writeValue(propertyName, propertyValue);

    }

    private boolean vectorEquals(Vector v1, Vector v2) {
        if (v1.size() != v2.size()) {
            return false;
        } else {
            for (int i = 0; i < v1.size(); ++i) {
                if (!v1.elementAt(i).equals(v2.elementAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Retrieves the set of rules being used by this property manager if any exist.
     *
     * @return The rulesets being used by this property manager
     */
    @Override
    public Vector getRules() {
        throw new RuntimeException("PropertyManager rules not implemented");
    }

    /**
     * Adds a set of rules to be used by this PropertyManager.
     * Note that rules sets are inclusive, they add new possible
     * values, never remove possible values.
     *
     * @param rules The set of rules to be added to the permitted list
     */
    @Override
    public void addRules(IPropertyRules rules) {
        throw new RuntimeException("PropertyManager rules not implemented");
    }


    public Vector getValue(String name) {
        try {
            Property p = (Property)properties.getRecordForValue("NAME", name);
            return p.value;
        } catch (NoSuchElementException nsee) {
            return null;
        }
    }

    private void writeValue(String propertyName, Vector value) {
        Property theProp = new Property();
        theProp.name = propertyName;
        theProp.value = value;

        Vector IDs = properties.getIDsForValue("NAME", propertyName);
        if (IDs.size() == 1) {
            theProp.setID((Integer)IDs.elementAt(0));
        }

        properties.write(theProp);
    }

}
