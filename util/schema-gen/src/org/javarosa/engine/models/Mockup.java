/**
 * 
 */
package org.javarosa.engine.models;

import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;

/**
 * @author ctsims
 *
 */
public class Mockup {
	String XMLNS = "http://javarosa.org/mockup";
	
	Hashtable<String, DataInstance> instances;
	Date date;
	Vector<Session> sessions;
	
	public Mockup() {
		
	}
	
	public Hashtable<String, DataInstance> getInstances() {
		return instances;
	}
	
	public Date getDate() {
		return date;
	}
	
	public Session[] getSessions() {
		return sessions.toArray(new Session[0]);
	}
	
	public void addSession(Session session) {
		this.sessions.addElement(session);
	}
}
