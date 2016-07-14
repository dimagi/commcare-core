package org.commcare.session;

import org.commcare.suite.model.StackFrameStep;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class SessionInstanceBuilder {
    public static FormInstance getSessionInstance(SessionFrame frame, String deviceId,
                                                  String appversion, String username,
                                                  String userId,
                                                  Hashtable<String, String> userFields) {
        TreeElement sessionRoot = new TreeElement("session", 0);

        addDatums(sessionRoot, frame);
        addMetadata(sessionRoot, deviceId, appversion, username, userId);
        addUserProperties(sessionRoot, userFields);

        return new FormInstance(sessionRoot, "session");
    }

    private static void addDatums(TreeElement sessionRoot, SessionFrame frame) {
        TreeElement sessionData = new TreeElement("data", 0);

        sessionRoot.addChild(sessionData);
        for (StackFrameStep step : frame.getSteps()) {
            if (SessionFrame.STATE_DATUM_VAL.equals(step.getType())) {
                Vector<TreeElement> matchingElements =
                        sessionData.getChildrenWithName(step.getId());

                if (matchingElements.size() > 0) {
                    matchingElements.elementAt(0).setValue(new UncastData(step.getValue()));
                } else {
                    TreeElement datum = new TreeElement(step.getId());
                    datum.setValue(new UncastData(step.getValue()));
                    sessionData.addChild(datum);
                }
            }
        }
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
