package org.commcare.suite.model;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by wpride1 on 4/17/15.
 *
 * Evaluated form of Callout class where all XPaths have been processed.
 */
public class CalloutData {
    final String actionName;
    final String image;
    final String displayName;
    final String type;
    final Hashtable<String, String> extras;
    final Vector<String> responses;

    public CalloutData(String actionName, String image, String displayName, Hashtable<String, String> extras, Vector<String> responses, String type) {
        this.actionName = actionName;
        this.image = image;
        this.displayName = displayName;
        this.extras = extras;
        this.responses = responses;
        this.type = type;
    }

    public String getImage() {
        return image;
    }

    public String getActionName() {
        return actionName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Hashtable<String, String> getExtras() {
        return extras;
    }

    public Vector<String> getResponses() {
        return responses;
    }

    public String getType() {
        return type;
    }
}
