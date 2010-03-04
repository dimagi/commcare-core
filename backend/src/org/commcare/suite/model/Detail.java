/**
 * 
 */
package org.commcare.suite.model;

import java.util.Vector;

import org.javarosa.core.model.instance.FormInstance;

/**
 * @author ctsims
 *
 */
public class Detail {
	String id;
	
	FormInstance context;
	
	Vector<Text> headers;
	Vector<Text> templates;
	
	/**
	 * Serialization Only
	 */
	public Detail() {
		
	}
	
	public Detail(String id, FormInstance context, Vector<Text> headers, Vector<Text> templates) {
		this.id = id;
		this.context = context;
		this.headers = headers;
		this.templates = templates;
	}
	
	public String getId() {
		return id;
	}
	
}
