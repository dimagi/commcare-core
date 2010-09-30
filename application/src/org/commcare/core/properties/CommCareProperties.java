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
	
	// http, since it doesn't go in transport layer anymore
    public final static String POST_URL_PROPERTY = "PostURL";
    
	// http, since it doesn't go in transport layer anymore
    public final static String POST_URL_TEST_PROPERTY = "PostTestURL";
    
    //auto-purging
    public final static String PURGE_LAST = "last-purge";
    public final static String PURGE_FREQ = "purge-freq";
    
    // First Run
    public final static String IS_FIRST_RUN = "cc-first-run";
    public final static String FIRST_RUN_YES = "Yes";
    public final static String FIRST_RUN_NO = "No";
    
    //OTA Restore
    public final static String OTA_RESTORE_URL = "ota-restore-url";
    public final static String OTA_RESTORE_TEST_URL = "ota-restore-url-testing";
    
    //User registration
    public final static String USER_REG_NAMESPACE = "user_reg_namespace";
    
    public final static String SEND_STYLE ="cc-send-procedure";
    	
    public final static String SEND_STYLE_HTTP ="cc-send-http";
    	
    public final static String SEND_STYLE_FILE ="cc-send-file";
    	
    public final static String SEND_STYLE_NONE ="cc-send-none";
    
    //Number of days before a form can be deleted
    public final static String DAYS_FOR_REVIEW = "cc-review-days";
    
    public final static String OTA_RESTORE_OFFLINE = "cc-restore-offline-file";
    
	/**
	 * Creates the JavaRosa set of property rules
	 */
	public CommCareProperties() {
		rules = new Hashtable();
		readOnlyProperties = new Vector();
		
		Vector firstRun = new Vector();
		firstRun.addElement(FIRST_RUN_YES);
		firstRun.addElement(FIRST_RUN_NO);
		
		rules.put(IS_FIRST_RUN, firstRun);
		
        rules.put(COMMCARE_VERSION, new Vector());
        readOnlyProperties.addElement(COMMCARE_VERSION);
        
        // PostURL List Property
        rules.put(POST_URL_PROPERTY, new Vector());
        rules.put(POST_URL_TEST_PROPERTY, new Vector());
        
        rules.put(PURGE_LAST, new Vector());
        rules.put(PURGE_FREQ, new Vector());
        readOnlyProperties.addElement(PURGE_LAST);
        
        rules.put(OTA_RESTORE_URL, new Vector());
        rules.put(OTA_RESTORE_TEST_URL,new Vector());
        
        rules.put(USER_REG_NAMESPACE, new Vector());
        readOnlyProperties.addElement(USER_REG_NAMESPACE);
        
        Vector sendStyles = new Vector();
        sendStyles.addElement(SEND_STYLE_HTTP);
        sendStyles.addElement(SEND_STYLE_FILE);
        sendStyles.addElement(SEND_STYLE_NONE);
        
        rules.put(SEND_STYLE, sendStyles);
        
        rules.put(DAYS_FOR_REVIEW, new Vector());
        
        rules.put(OTA_RESTORE_OFFLINE, new Vector());
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
		} else if (PURGE_LAST.equals(propertyName)) {
        	return "Last Purge on";
        } else if (PURGE_FREQ.equals(propertyName)) {
        	return "Purge Freq. (days)";
        } else if (POST_URL_TEST_PROPERTY.equals(propertyName)) {
        	return "Testing Submission URL";
        } else if (POST_URL_PROPERTY.equals(propertyName)) {
        	return "Form Submission URL";
        } else if (IS_FIRST_RUN.equals(propertyName)) {
        	return "First Run Screen at Startup?";
        } else if (OTA_RESTORE_URL.equals(propertyName)) {
        	return "URL of OTA Restore Server";
        } else if (OTA_RESTORE_TEST_URL.equals(propertyName)) {
        	return "URL of OTA Restore Testing Server";
        } else if(SEND_STYLE.equals(propertyName)) {
        	return "Form Send/Save Process";
        } else if (OTA_RESTORE_OFFLINE.equals(propertyName)) {
        	return "Offline File Ref for OTA Bypass";
        }
    	return propertyName;
	}

	public String getHumanReadableValue(String propertyName, String value) {
		if(SEND_STYLE.equals(propertyName)) {
        	if(SEND_STYLE_HTTP.equals(value)) {
        		return "HTTP Post";
        	} else if(SEND_STYLE_FILE.equals(value)) {
        		return "Save to File";
        	} else if(SEND_STYLE_NONE.equals(value)) {
        		return "Don't Save";
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
