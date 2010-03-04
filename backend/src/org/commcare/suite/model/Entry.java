/**
 * 
 */
package org.commcare.suite.model;

import java.util.Hashtable;

/**
 * @author ctsims
 *
 */
public class Entry {
	private String xFormNamespace;
	private Hashtable<String, String> references;
	private String shortDetailId;
	private String longDetailId;
	private Text commandText;
	private String commandId;
	
	public Entry(String commandId, Text commandText, String longDetailId,
			String shortDetailId, Hashtable<String, String> references,
			String formNamespace) {
		this.commandId = commandId;
		this.commandText = commandText;
		this.longDetailId = longDetailId;
		this.references = references;
		this.shortDetailId = shortDetailId;
		xFormNamespace = formNamespace;
	}
	
	public String getCommandId() {
		return commandId;
	}
	
	
	
}
