package org.javarosa.core.model.actions;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author ctsims
 */
public abstract class Action implements Externalizable {

    // Events that can trigger an action
    public static final String EVENT_XFORMS_READY = "xforms-ready";
    public static final String EVENT_XFORMS_MODEL_CONSTRUCT_DONE = "xforms-model-construct-done";
    public static final String EVENT_XFORMS_REVALIDATE = "xforms-revalidate";
    public static final String EVENT_JR_INSERT = "jr-insert";
    public static final String EVENT_QUESTION_VALUE_CHANGED = "xforms-value-changed";
    private static final String[] allEvents = new String[]{EVENT_JR_INSERT,
            EVENT_QUESTION_VALUE_CHANGED, EVENT_XFORMS_READY, EVENT_XFORMS_REVALIDATE, EVENT_XFORMS_MODEL_CONSTRUCT_DONE};

    private String name;

    public Action() {
    }

    public Action(String name) {
        this.name = name;
    }

    /**
     * Process actions that were triggered in the form.
     *
     * NOTE: Currently actions are only processed on nodes that are
     * WITHIN the context provided, if one is provided. This will
     * need to get changed possibly for future action types.
     *
     * @return TreeReference targeted by the action or null if the action
     * wasn't completed.
     */
    public abstract TreeReference processAction(FormDef model, TreeReference context);

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        name = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, name);
    }

    public static boolean isValidEvent(String actionEventAttribute)  {
        for (String event : allEvents) {
            if (event.equals(actionEventAttribute)) {
                return true;
            }
        }
        return false;
    }

}
