/**
 * 
 */
package org.commcare.suite.model;

import java.util.Hashtable;

import org.commcare.resources.model.ResourceTable;

/**
 * @author ctsims
 *
 */
public class Suite {
	//Resource table isn't persisted, it is linked.
	private ResourceTable resourceTable;
	
	private int version;
	
	private Hashtable<String, Detail> details;
	private Hashtable<String, Entry> entries;
	
	public Suite(ResourceTable resourceTable, int version, Hashtable<String, Detail> details, Hashtable<String, Entry> entries) {
		this.resourceTable = resourceTable;
		this.version = version;
		this.details = details;
		this.entries = entries;
	}
	
	
}
