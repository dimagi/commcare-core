package org.javarosa.core.model;

import org.javarosa.core.model.instance.TreeReference;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by amstone326 on 2/2/16.
 */
public class ActionTriggerSource {

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

    public void triggerActionsFromEvent(String event, FormDef model, TreeReference contextForAction) {
        triggerActionsFromEvent(event, model, contextForAction, null, null);
    }

    public void triggerActionsFromEvent(String event, FormDef model, TreeReference contextForAction,
                                        Method applyToResultingRef, Object methodCaller) {
        for (Action action : getListenersForEvent(event)) {
            TreeReference refSetByAction = action.processAction(model, contextForAction);
            if (applyToResultingRef != null) {
                try {
                    applyToResultingRef.invoke(methodCaller, refSetByAction);
                } catch(IllegalAccessException | InvocationTargetException e) {

                }
            }
        }

    }

}
