package org.javarosa.core.services.properties;

import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.Localizer;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A set of rules governing the allowable properties for JavaRosa's
 * core funtionality.
 *
 * @author ctsims
 */
public class JavaRosaPropertyRules implements IPropertyRules {
    final Hashtable rules;

    final Vector readOnlyProperties;

    public final static String DEVICE_ID_PROPERTY = "DeviceID";
    public final static String CURRENT_LOCALE = "cur_locale";

    public final static String LOGS_ENABLED = "logenabled";

    public final static String LOGS_ENABLED_YES = "Enabled";
    public final static String LOGS_ENABLED_NO = "Disabled";

    /**
     * The expected compliance version for the OpenRosa API set *
     */
    public final static String OPENROSA_API_LEVEL = "jr_openrosa_api";

    /**
     * Creates the JavaRosa set of property rules
     */
    public JavaRosaPropertyRules() {
        rules = new Hashtable();
        readOnlyProperties = new Vector();

        //DeviceID Property
        rules.put(DEVICE_ID_PROPERTY, new Vector());
        Vector logs = new Vector();
        logs.addElement(LOGS_ENABLED_NO);
        logs.addElement(LOGS_ENABLED_YES);
        rules.put(LOGS_ENABLED, logs);

        rules.put(CURRENT_LOCALE, new Vector());

        rules.put(OPENROSA_API_LEVEL, new Vector());

        readOnlyProperties.addElement(DEVICE_ID_PROPERTY);
        readOnlyProperties.addElement(OPENROSA_API_LEVEL);

    }

    public Vector allowableValues(String propertyName) {
        if (CURRENT_LOCALE.equals(propertyName)) {
            Localizer l = Localization.getGlobalLocalizerAdvanced();
            Vector v = new Vector();
            String[] locales = l.getAvailableLocales();
            for (int i = 0; i < locales.length; ++i) {
                v.addElement(locales[i]);
            }
            return v;
        }
        return (Vector)rules.get(propertyName);
    }

    public boolean checkValueAllowed(String propertyName, String potentialValue) {
        if (CURRENT_LOCALE.equals(propertyName)) {
            return Localization.getGlobalLocalizerAdvanced().hasLocale(potentialValue);
        }
        Vector prop = ((Vector)rules.get(propertyName));
        if (prop.size() != 0) {
            //Check whether this is a dynamic property
            if (prop.size() == 1 && checkPropertyAllowed((String)prop.elementAt(0))) {
                // If so, get its list of available values, and see whether the potentival value is acceptable.
                return PropertyManager._().getProperty((String)prop.elementAt(0)).contains(potentialValue);
            } else {
                return ((Vector)rules.get(propertyName)).contains(potentialValue);
            }
        } else
            return true;
    }

    public Vector allowableProperties() {
        Vector propList = new Vector();
        Enumeration iter = rules.keys();
        while (iter.hasMoreElements()) {
            propList.addElement(iter.nextElement());
        }
        return propList;
    }

    public boolean checkPropertyAllowed(String propertyName) {
        Enumeration iter = rules.keys();
        while (iter.hasMoreElements()) {
            if (propertyName.equals(iter.nextElement())) {
                return true;
            }
        }
        return false;
    }

    public boolean checkPropertyUserReadOnly(String propertyName) {
        return readOnlyProperties.contains(propertyName);
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.properties.IPropertyRules#getHumanReadableDescription(java.lang.String)
     */
    public String getHumanReadableDescription(String propertyName) {
        if (DEVICE_ID_PROPERTY.equals(propertyName)) {
            return "Unique Device ID";
        } else if (LOGS_ENABLED.equals(propertyName)) {
            return "Device Logging";
        } else if (CURRENT_LOCALE.equals(propertyName)) {
            return Localization.get("settings.language");
        } else if (OPENROSA_API_LEVEL.equals(propertyName)) {
            return "OpenRosa API Level";
        }
        return propertyName;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.properties.IPropertyRules#getHumanReadableValue(java.lang.String, java.lang.String)
     */
    public String getHumanReadableValue(String propertyName, String value) {
        if (CURRENT_LOCALE.equals(propertyName)) {
            String name = Localization.getGlobalLocalizerAdvanced().getText(value);
            if (name != null) {
                return name;
            }
        }
        return value;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.properties.IPropertyRules#handlePropertyChanges(java.lang.String)
     */
    public void handlePropertyChanges(String propertyName) {
        if (CURRENT_LOCALE.equals(propertyName)) {
            String locale = PropertyManager._().getSingularProperty(propertyName);
            Localization.setLocale(locale);
        }
    }
}
