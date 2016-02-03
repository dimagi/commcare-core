package org.javarosa.core.model;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by amstone326 on 2/2/16.
 */
public abstract class ActionTriggerSource {

    // map from an event to the actions it should trigger
    Hashtable<String, Vector<Action>> eventListeners;

    public Vector<Action> getListenersForEvent(String event) {
        if (this.eventListeners.containsKey(event)) {
            return eventListeners.get(event);
        }
        return new Vector<Action>();
    }

    public void registerEventListener(String event, Action action) {
        Vector<Action> actions;

        if (eventListeners.containsKey(event)) {
            actions = eventListeners.get(event);
        } else {
            actions = new Vector<Action>();
            eventListeners.put(event, actions);
        }
        actions.addElement(action);
    }

    public void triggerActionsFromEvent(String event) {
        for (Action action : getListenersForEvent(event)) {
            action.processAction(this, null);
        }
    }

}
