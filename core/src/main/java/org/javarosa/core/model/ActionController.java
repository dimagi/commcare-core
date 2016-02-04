package org.javarosa.core.model;

import org.javarosa.core.model.instance.TreeReference;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Registers actions that should be triggered by certain events, and handles the triggering
 * of those actions when an event occurs.
 *
 * Created by amstone326 on 2/2/16.
 */
public class ActionController {

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
        triggerActionsFromEvent(event, model, contextForAction, null);
    }

    public void triggerActionsFromEvent(String event, FormDef model, TreeReference contextForAction,
                                        ActionResultProcessor resultProcessor) {
        for (Action action : getListenersForEvent(event)) {
            TreeReference refSetByAction = action.processAction(model, contextForAction);
            if (resultProcessor != null && refSetByAction != null) {
                resultProcessor.processResultOfAction(refSetByAction, event);
            }
        }
    }

    public interface ActionResultProcessor {
        /**
         * @param targetRef - the ref that this action targeted
         * @param event - the event that triggered this action
         */
        void processResultOfAction(TreeReference targetRef, String event);
    }

}
