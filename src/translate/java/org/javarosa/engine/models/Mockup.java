/**
 *
 */
package org.javarosa.engine.models;

import org.javarosa.core.model.instance.FormInstance;

import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author ctsims
 *
 */
public class Mockup {
    final Hashtable<String, FormInstance> instances;
    Date date;
    final Vector<Session> sessions;

    public Mockup() {
        sessions = new Vector<>();
        instances = new Hashtable<>();
    }

    public Hashtable<String, FormInstance> getInstances() {
        return instances;
    }

    public Date getDate() {
        return date;
    }

    public MockupEditor getEditor() {
        return new MockupEditor(this);
    }

    public class MockupEditor {
        final Mockup m;
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
