package org.commcare.core.properties;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.IPropertyRules;

public class CommCareProperties implements IPropertyRules {
	Hashtable rules;
	Vector readOnlyProperties;

	public final static String COMMCARE_VERSION = "app-version";
	
	public static final String BACKUP_URL = "backup-url";
	public static final String RESTORE_URL = "restore-url";
    public static final String BACKUP_MODE = "BackupMode";

    // Backup Modes
    public static final String BACKUP_MODE_HTTP = "http_mode";
    public static final String BACKUP_MODE_FILE = "file_mode";
    
	// http, since it doesn't go in transport layer anymore
    public final static String POST_URL_PROPERTY = "PostURL";

    //auto-purging
    public final static String PURGE_LAST = "last-purge";
    public final static String PURGE_FREQ = "purge-freq";
    
	/**
	 * Creates the JavaRosa set of property rules
	 */
	public CommCareProperties() {
		rules = new Hashtable();
		readOnlyProperties = new Vector();
		
        rules.put(COMMCARE_VERSION, new Vector());
        readOnlyProperties.addElement(COMMCARE_VERSION);
		
        rules.put(BACKUP_URL, new Vector());
        rules.put(RESTORE_URL, new Vector());

        Vector allowableModes = new Vector();
        allowableModes.addElement(BACKUP_MODE_HTTP);
        allowableModes.addElement(BACKUP_MODE_FILE);
        rules.put(BACKUP_MODE, allowableModes);	
        
        // PostURL List Property
        rules.put(POST_URL_PROPERTY, new Vector());
        
        rules.put(PURGE_LAST, new Vector());
        rules.put(PURGE_FREQ, new Vector());
        readOnlyProperties.addElement(PURGE_LAST);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.javarosa.properties.IPropertyRules#allowableValues(String)
	 */
	public Vector allowableValues(String propertyName) {
		return (Vector) rules.get(propertyName);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.javarosa.properties.IPropertyRules#checkValueAllowed(String,
	 *      String)
	 */
	public boolean checkValueAllowed(String propertyName, String potentialValue) {
		if (PURGE_FREQ.equals(propertyName)) {
			return (parsePurgeFreq(potentialValue) != -1);
		}
		
		Vector prop = ((Vector) rules.get(propertyName));
		if (prop.size() != 0) {
			// Check whether this is a dynamic property
			if (prop.size() == 1
					&& checkPropertyAllowed((String) prop.elementAt(0))) {
				// If so, get its list of available values, and see whether the
				// potential value is acceptable.
				return ((Vector) PropertyManager._().getProperty((String) prop.elementAt(0))).contains(potentialValue);
			} else {
				return ((Vector) rules.get(propertyName)).contains(potentialValue);
			}
		} else
			return true;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.javarosa.properties.IPropertyRules#allowableProperties()
	 */
	public Vector allowableProperties() {
		Vector propList = new Vector();
		Enumeration iter = rules.keys();
		while (iter.hasMoreElements()) {
			propList.addElement(iter.nextElement());
		}
		return propList;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.javarosa.properties.IPropertyRules#checkPropertyAllowed)
	 */
	public boolean checkPropertyAllowed(String propertyName) {
		Enumeration iter = rules.keys();
		while (iter.hasMoreElements()) {
			if (propertyName.equals(iter.nextElement())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.javarosa.properties.IPropertyRules#checkPropertyUserReadOnly)
	 */
	public boolean checkPropertyUserReadOnly(String propertyName) {
		return readOnlyProperties.contains(propertyName);
	}

	public String getHumanReadableDescription(String propertyName) {
		if(COMMCARE_VERSION.equals(propertyName)) {
			return "CommCare Version";
		} else if (BACKUP_URL.equals(propertyName)) {
     		return "Backup URL";
     	} else if (RESTORE_URL.equals(propertyName)) {
     		return "Restore URL";
        } else if (BACKUP_MODE.equals(propertyName)) {
            return "Backup Mode";
        } else if (PURGE_LAST.equals(propertyName)) {
        	return "Last Purge on";
        } else if (PURGE_FREQ.equals(propertyName)) {
        	return "Purge Freq. (days)";
        }
    	return propertyName;
	}

	public String getHumanReadableValue(String propertyName, String value) {
        if(BACKUP_MODE.equals(propertyName)) {
            if(BACKUP_MODE_HTTP.equals(value)) {
                return "Over the Air";
            } else if(BACKUP_MODE_FILE.equals(value)) {
                return "Memory Card"; 
            }
        }
        return value;
	}

	public void handlePropertyChanges(String propertyName) {
		// nothing.  
		// what's this method for?
	}	
	
	//0 == always purge
	//-1 == error
	//n > 0 == purge if last purge was at least n days ago
	public static int parsePurgeFreq (String freqStr) {
		if (freqStr == null) {
			return 0;
		}
		
		freqStr = freqStr.trim();
		if (freqStr.length() == 0) {
			return 0;
		} else {
			int n;
			try {
				n = Integer.parseInt(freqStr);
				if (n < 0)
					n = -1;
			} catch (NumberFormatException nfe) {
				n = -1;
			}
			return n;
		}
	}
	
	public static Date parseLastPurge (String lastStr) {
		if (lastStr == null) {
			return null;
		}
		
		return DateUtils.parseDateTime(lastStr.trim());
	}
}
