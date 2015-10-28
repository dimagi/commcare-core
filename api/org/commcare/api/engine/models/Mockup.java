/**
 *
 */
package org.commcare.api.engine.models;

import org.javarosa.core.model.instance.FormInstance;

import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author ctsims
 *
 */
public class Mockup {
    Hashtable<String, FormInstance> instances;
    Date date;
    Vector<Session> sessions;

    public Mockup() {
        sessions = new Vector<Session>();
        instances = new Hashtable<String, FormInstance>();
    }

    public Hashtable<String, FormInstance> getInstances() {
        return instances;
    }

    public Date getDate() {
        return date;
    }

    public Session[] getSessions() {
        return sessions.toArray(new Session[0]);
    }

    public MockupEditor getEditor() {
        return new MockupEditor(this);
    }

    public class MockupEditor {
        Mockup m;
        private MockupEditor(Mockup m) {
            this.m = m;
        }

        public void commit() {

        }

        public void setDate(Date d) {
            m.date = d;
        }

        public void addInstance(FormInstance instance) {
            m.instances.put(instance.getInstanceId(), instance);
        }

        public void addSession(Session s) {
            m.sessions.add(s);
        }
    }
}
