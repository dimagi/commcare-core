package org.javarosa.core.model;

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
public class Action implements Externalizable {

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
    public TreeReference processAction(ActionTriggerSource target, TreeReference context) {
        return null;
    }

    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        name = ExtUtil.readString(in);
    }

    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, name);
    }

    // Events that can trigger an action
    public enum Event {

        EVENT_XFORMS_READY("xforms-ready"),
        EVENT_XFORMS_REVALIDATE("xforms-revalidate"),
        EVENT_JR_INSERT("jr-insert"),
        EVENT_QUESTION_VALUE_CHANGED("xforms-value-changed");

        String nodeName;

        Event(String s) {
            this.nodeName = s;
        }

        public static boolean isValidEvent(String s)  {
            for (Event e : values()) {
                if (e.nodeName.equals(s)) {
                    return true;
                }
            }
            return false;
        }
    }
}
