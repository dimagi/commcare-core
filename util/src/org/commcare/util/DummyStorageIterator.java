/**
 * 
 */
package org.commcare.util;

import java.util.Enumeration;
import java.util.Hashtable;

import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.util.externalizable.Externalizable;

/**
 * @author ctsims
 *
 */
public class DummyStorageIterator implements IStorageIterator {
	Hashtable<Integer, Externalizable> data;
	int count;
	Object[] keys;
	

	public DummyStorageIterator(Hashtable<Integer, Externalizable> data) {
		this.data = data;
		keys = data.keySet().toArray();
		count = 0;
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageIterator#hasMore()
	 */
	public boolean hasMore() {
		return count < keys.length;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageIterator#nextID()
	 */
	public int nextID() {
		count++;
		return (Integer)keys[count -1]; 
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageIterator#nextRecord()
	 */
	public Externalizable nextRecord() {
		return data.get(nextID());
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.IStorageIterator#numRecords()
	 */
	public int numRecords() {
		return data.size();
	}

	public int peekID() {
		return (Integer)keys[count];
	}

}
