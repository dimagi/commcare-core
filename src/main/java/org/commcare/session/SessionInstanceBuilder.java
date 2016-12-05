package org.commcare.session;

import org.commcare.suite.model.StackFrameStep;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.util.OrderedHashtable;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class SessionInstanceBuilder {
    public static final String KEY_LAST_QUERY_STRING = "LAST_QUERY_STRING";
    public static final String KEY_ENTITY_LIST_EXTRA_DATA = "entity-list-data";

    public static FormInstance getSessionInstance(SessionFrame frame, String deviceId,
                                                  String appversion, String username,
                                                  String userId,
                                                  Hashtable<String, String> userFields) {
        TreeElement sessionRoot = new TreeElement("session", 0);

        addSessionNavData(sessionRoot, frame);
        addMetadata(sessionRoot, deviceId, appversion, username, userId);
        addUserProperties(sessionRoot, userFields);

        return new FormInstance(sessionRoot, "session");
    }

    private static void addSessionNavData(TreeElement sessionRoot, SessionFrame frame) {
        TreeElement sessionData = new TreeElement("data", 0);
        addDatums(sessionData, frame);
        addUserQueryData(sessionData, frame);
        sessionRoot.addChild(sessionData);
    }

    /**
     * Add datums chosen by user to the session
     */
    private static void addDatums(TreeElement sessionData, SessionFrame frame) {
        for (StackFrameStep step : frame.getSteps()) {
            if (SessionFrame.STATE_DATUM_VAL.equals(step.getType()) ||
                    SessionFrame.STATE_DATUM_COMPUTED.equals(step.getType())) {
                Vector<TreeElement> matchingElements =
                        sessionData.getChildrenWithName(step.getId());
                if (matchingElements.size() > 0) {
                    matchingElements.elementAt(0).setValue(new UncastData(step.getValue()));
                } else {
                    addData(sessionData, step.getId(), step.getValue());
                }
            }
        }
    }

    /**
     * Add data to session tracking queries user made before entering form
     */
    private static void addUserQueryData(TreeElement sessionData, SessionFrame frame) {
        for (StackFrameStep step : frame.getSteps()) {
            String textSearch = getStringQuery(step);
            if (textSearch != null) {
                addData(sessionData, "stringquery", textSearch);
            }

            String calloutResultCount = getCalloutSearchResultCount(step);
            if (calloutResultCount != null) {
                addData(sessionData, "fingerprintquery", calloutResultCount);
            }
        }
    }

    private static String getStringQuery(StackFrameStep step) {
        Object extra = step.getExtra(KEY_LAST_QUERY_STRING);
        if (extra != null && extra instanceof String && !"".equals(extra)) {
            return (String) extra;
        }
        return null;
    }

    private static String getCalloutSearchResultCount(StackFrameStep step) {
        Object entitySelectCalloutSearch = step.getExtra(KEY_ENTITY_LIST_EXTRA_DATA);
        if (entitySelectCalloutSearch != null && entitySelectCalloutSearch instanceof OrderedHashtable) {
            return "" + ((OrderedHashtable)entitySelectCalloutSearch).keySet().size();
        }
        return null;
    }


    private static void addMetadata(TreeElement sessionRoot, String deviceId,
                                    String appversion, String username,
                                    String userId) {
        TreeElement sessionMeta = new TreeElement("context", 0);

        addData(sessionMeta, "deviceid", deviceId);
        addData(sessionMeta, "appversion", appversion);
        addData(sessionMeta, "username", username);
        addData(sessionMeta, "userid", userId);

        sessionRoot.addChild(sessionMeta);
    }

    private static void addUserProperties(TreeElement sessionRoot,
                                          Hashtable<String, String> userFields) {
        TreeElement user = new TreeElement("user", 0);
        TreeElement userData = new TreeElement("data", 0);
        user.addChild(userData);
        for (Enumeration en = userFields.keys(); en.hasMoreElements(); ) {
            String key = (String)en.nextElement();
            addData(userData, key, userFields.get(key));
        }

        sessionRoot.addChild(user);

    }

    private static void addData(TreeElement root, String name, String data) {
        TreeElement datum = new TreeElement(name);
        datum.setValue(new UncastData(data));
        root.addChild(datum);
    }

}
