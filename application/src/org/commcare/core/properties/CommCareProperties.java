package org.commcare.core.properties;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.model.PeriodicEvent;
import org.commcare.util.time.AutoSyncEvent;
import org.commcare.util.time.AutoUpdateEvent;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.IPropertyRules;

public class CommCareProperties implements IPropertyRules {
    Hashtable rules;
    Vector readOnlyProperties;

    public final static String COMMCARE_VERSION = "app-version";

    public final static String DEPLOYMENT_MODE = "deployment";
    public final static String DEPLOY_TESTING = "deploy-test";
    public final static String DEPLOY_RELEASE = "deploy-rel";
    public final static String DEPLOY_DEFAULT = "deploy-def";

    // http, since it doesn't go in transport layer anymore
    public final static String POST_URL_PROPERTY = "PostURL";

    // http, since it doesn't go in transport layer anymore
    public final static String POST_URL_TEST_PROPERTY = "PostTestURL";

    //auto-purging
    public final static String PURGE_LAST = "last-purge";
    public final static String PURGE_FREQ = "purge-freq";

    // First Run
    public final static String IS_FIRST_RUN = "cc-first-run";
    public final static String PROPERTY_YES = "Yes";
    public final static String PROPERTY_NO = "No";

    //OTA Restore
    public final static String OTA_RESTORE_URL = "ota-restore-url";
    public final static String OTA_RESTORE_TEST_URL = "ota-restore-url-testing";
    public final static String LAST_SUCCESSFUL_SYNC = "last-sync-token";
    public final static String LAST_SYNC_AT = "last-sync-timestamp";

    public final static String RESTORE_TOLERANCE = "restore-tolerance";
    public final static String REST_TOL_STRICT = "strict";
    public final static String REST_TOL_LOOSE = "loose";

    public final static String DEMO_MODE = "demo-mode";
    public final static String DEMO_ENABLED = "yes";
    public final static String DEMO_DISABLED = "no";

    public final static String TETHER_MODE = "server-tether";
    public final static String TETHER_PUSH_ONLY = "push-only";
    public final static String TETHER_SYNC = "sync";

    //User registration
    public final static String USER_REG_NAMESPACE = "user_reg_namespace";

    //Whether users need to register with the server
    public final static String USER_REG_TYPE = "user_reg_server";
    public final static String USER_REG_REQUIRED = "required";
    public final static String USER_REG_SKIP = "skip";

    public final static String SEND_STYLE ="cc-send-procedure";

    public final static String SEND_STYLE_HTTP ="cc-send-http";

    public final static String SEND_STYLE_FILE ="cc-send-file";

    public final static String SEND_STYLE_NONE ="cc-send-none";

    //Number of days before a form can be deleted
    public final static String DAYS_FOR_REVIEW = "cc-review-days";

    public final static String PASSWORD_FORMAT = "password_format";

    public final static String ENTRY_MODE = "cc-entry-mode";
    public final static String ENTRY_MODE_QUICK = "cc-entry-quick";
    public final static String ENTRY_MODE_REVIEW = "cc-entry-review";

    public final static String SEND_UNSENT_STYLE = "cc-send-unsent";
    public final static String SEND_UNSENT_MANUAL = "cc-su-man";
    public final static String SEND_UNSENT_AUTOMATIC = "cc-su-auto";

    public final static String OTA_RESTORE_OFFLINE = "cc-restore-offline-file";

    public final static String LOGIN_IMAGE = "cc_login_image";

    public final static String USER_DOMAIN = "cc_user_domain";

    public final static String LOGIN_MODE = "cc-user-mode";
    public final static String LOGIN_MODE_NORMAL = "cc-u-normal";
    public final static String LOGIN_MODE_AUTO = "cc-u-auto";

    public final static String LOGGED_IN_USER = "cc-u-logged-in";

    public final static String CONTENT_VALIDATED = "cc-content-valid";

    public final static String LOGIN_IMAGES = "cc-login-images";

    public final static String AUTO_UPDATE_FREQUENCY = "cc-autoup-freq";
    public final static String FREQUENCY_NEVER = "freq-never";
    public final static String FREQUENCY_DAILY = "freq-daily";
    public final static String FREQUENCY_WEEKLY = "freq-weekly";
    public static final String AUTO_SYNC_FREQUENCY = "cc-autosync-freq";
    public static final String UNSENT_FORM_NUMBER_LIMIT = "form-number-limit";
    public static final String UNSENT_FORM_TIME_LIMIT = "form-time-limit";

    public final static String INSTALL_RETRY_ATTEMPTS = "cc-in-retry-attempts";


    /**
     * Creates the JavaRosa set of property rules
     */
    public CommCareProperties() {
        rules = new Hashtable();
        readOnlyProperties = new Vector();

        Vector yesNo = new Vector();
        yesNo.addElement(PROPERTY_YES);
        yesNo.addElement(PROPERTY_NO);

        rules.put(IS_FIRST_RUN, yesNo);

        rules.put(COMMCARE_VERSION, new Vector());
        readOnlyProperties.addElement(COMMCARE_VERSION);

        Vector deployment_modes = new Vector();
        deployment_modes.addElement(DEPLOY_DEFAULT);
        deployment_modes.addElement(DEPLOY_TESTING);
        deployment_modes.addElement(DEPLOY_RELEASE);
        rules.put(DEPLOYMENT_MODE, deployment_modes);

        // PostURL List Property
        rules.put(POST_URL_PROPERTY, new Vector());
        rules.put(POST_URL_TEST_PROPERTY, new Vector());

        rules.put(PURGE_LAST, new Vector());
        rules.put(PURGE_FREQ, new Vector());
        readOnlyProperties.addElement(PURGE_LAST);

        rules.put(OTA_RESTORE_URL, new Vector());
        rules.put(OTA_RESTORE_TEST_URL,new Vector());
        rules.put(LAST_SUCCESSFUL_SYNC, new Vector());
        rules.put(LAST_SYNC_AT, new Vector());

        rules.put(UNSENT_FORM_TIME_LIMIT, new Vector());
        rules.put(UNSENT_FORM_NUMBER_LIMIT, new Vector());


        readOnlyProperties.addElement(LAST_SUCCESSFUL_SYNC);
        readOnlyProperties.addElement(LAST_SYNC_AT);
        readOnlyProperties.addElement(UNSENT_FORM_TIME_LIMIT);
        readOnlyProperties.addElement(UNSENT_FORM_NUMBER_LIMIT);

        Vector vTol = new Vector();
        vTol.addElement(REST_TOL_STRICT);
        vTol.addElement(REST_TOL_LOOSE);
        rules.put(RESTORE_TOLERANCE, vTol);
        readOnlyProperties.addElement(RESTORE_TOLERANCE);

        Vector vDemo = new Vector();
        vDemo.addElement(DEMO_ENABLED);
        vDemo.addElement(DEMO_DISABLED);
        rules.put(DEMO_MODE, vDemo);

        Vector vTeth = new Vector();
        vTeth.addElement(TETHER_PUSH_ONLY);
        vTeth.addElement(TETHER_SYNC);
        rules.put(TETHER_MODE, vTeth);
        readOnlyProperties.addElement(TETHER_MODE);

        rules.put(USER_REG_NAMESPACE, new Vector());
        readOnlyProperties.addElement(USER_REG_NAMESPACE);

        Vector sendStyles = new Vector();
        sendStyles.addElement(SEND_STYLE_HTTP);
        sendStyles.addElement(SEND_STYLE_FILE);
        sendStyles.addElement(SEND_STYLE_NONE);

        rules.put(SEND_STYLE, sendStyles);

        rules.put(DAYS_FOR_REVIEW, new Vector());

        rules.put(OTA_RESTORE_OFFLINE, new Vector());

        //TODO: This actually does have a limited set
        rules.put(PASSWORD_FORMAT, new Vector());
        Vector entrymode = new Vector();
        entrymode.addElement(ENTRY_MODE_QUICK);
        entrymode.addElement(ENTRY_MODE_REVIEW);

        Vector sendunsent = new Vector();
        sendunsent.addElement(SEND_UNSENT_AUTOMATIC);
        sendunsent.addElement(SEND_UNSENT_MANUAL);
        rules.put(SEND_UNSENT_STYLE, sendunsent);

        Vector userReg = new Vector();
        userReg.addElement(USER_REG_REQUIRED);
        userReg.addElement(USER_REG_SKIP);
        rules.put(USER_REG_TYPE, userReg);

        rules.put(ENTRY_MODE, entrymode);

        rules.put(LOGIN_IMAGE, new Vector());

        rules.put(USER_DOMAIN, new Vector());

        Vector userSettings = new Vector();
        userSettings.addElement(LOGIN_MODE_NORMAL);
        userSettings.addElement(LOGIN_MODE_AUTO);

        rules.put(LOGGED_IN_USER,new Vector());

        rules.put(LOGIN_MODE, userSettings);

        rules.put(CONTENT_VALIDATED, yesNo);

        rules.put(LOGIN_IMAGES, yesNo);

        Vector updateFreq = new Vector();
        updateFreq.addElement(FREQUENCY_NEVER);
        updateFreq.addElement(FREQUENCY_DAILY);
        updateFreq.addElement(FREQUENCY_WEEKLY);

        rules.put(AUTO_UPDATE_FREQUENCY, updateFreq);
        rules.put(AUTO_SYNC_FREQUENCY, updateFreq);

        rules.put(INSTALL_RETRY_ATTEMPTS, new Vector());


        readOnlyProperties.addElement(CONTENT_VALIDATED);
        readOnlyProperties.addElement(ENTRY_MODE);
        readOnlyProperties.addElement(SEND_UNSENT_STYLE);
        readOnlyProperties.addElement(LOGIN_IMAGE);
        readOnlyProperties.addElement(LOGGED_IN_USER);
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
        } else if (DEPLOYMENT_MODE.equals(propertyName)) {
            return "Deployment mode";
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
        } else if (DEMO_MODE.equals(propertyName)) {
            return "Demo Mode Enabled";
        } else if (LOGIN_MODE.equals(propertyName)) {
            return "User Login Mode";
        } else if (AUTO_UPDATE_FREQUENCY.equals(propertyName)) {
            return "Auto-Update Frequency";
        } else if (LOGIN_IMAGES.equals(propertyName)) {
            return "Display Login Images?";
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
        } else if (DEPLOYMENT_MODE.equals(propertyName)) {
            if (DEPLOY_TESTING.equals(value)) {
                return "Testing";
            } else if (DEPLOY_RELEASE.equals(value)) {
                return "Release";
            } else if (DEPLOY_DEFAULT.equals(value)) {
                return "JAD setting";
            }
        } else if (LOGIN_MODE.equals(propertyName)) {
            if(LOGIN_MODE_NORMAL.equals(value)) {
                return "Normal Login";
            } else if(LOGIN_MODE_AUTO.equals(value)) {
                return "Automatic Login";
            }
        } else if (AUTO_UPDATE_FREQUENCY.equals(propertyName)) {
            if(FREQUENCY_NEVER.equals(value)) {
                return "Never";
            } else if(FREQUENCY_DAILY.equals(value)) {
                return "Daily";
            } else if(FREQUENCY_WEEKLY.equals(value)) {
                return "Weekly";
            }
        }

        return value;
    }

    public void handlePropertyChanges(String propertyName) {
        if(AUTO_UPDATE_FREQUENCY.equals(propertyName)) {
            //It might need to reschedule.
            PeriodicEvent.schedule(new AutoUpdateEvent());
        }
        if(AUTO_SYNC_FREQUENCY.equals(propertyName)) {
            //It might need to reschedule.
            PeriodicEvent.schedule(new AutoSyncEvent());
        }
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
