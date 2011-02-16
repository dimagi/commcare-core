/**
 * 
 */
package org.commcare.model;

import java.util.Date;
import java.util.Hashtable;
import java.util.Stack;

import org.javarosa.core.api.State;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.j2me.view.J2MEDisplay;

/**
 * @author ctsims
 *
 */
public abstract class PeriodicWrapperState implements State {
	
	private Hashtable<String, PeriodicEvent> d;
	private Stack<Integer> toCheck;
	private Date now;
	
	IStorageUtility storage;
	
	public PeriodicWrapperState(PeriodicEvent[] descriptors) {
		
		now = new Date();
		
		d = new Hashtable<String, PeriodicEvent>();
		for(PeriodicEvent e : descriptors) {
			d.put(e.getEventKey(), e);
		}
		
		storage = StorageManager.getStorage(PeriodicEventRecord.STORAGE_KEY);
		toCheck = new Stack<Integer>();
		for(IStorageIterator i = storage.iterate(); i.hasMore(); ) {
			toCheck.addElement(new Integer(i.nextID()));
		}
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.api.State#start()
	 */
	public void start() {
		next();
	}
	
	protected void returnFromEvent(PeriodicEvent e, PeriodicEventRecord record) {
		PeriodicEventRecord nextEvent = e.scheduleNextTrigger(record);
		if(nextEvent == null) {
			storage.remove(record);
		} else {
			try {
				storage.write(nextEvent);
			} catch (StorageFullException e1) {
				Logger.exception("In periodic wrapper", e1);
			}
		}
		next();
	}
	
	private void next(){
		if(toCheck.empty()) {
			done();
		} else {
			PeriodicEventRecord record = (PeriodicEventRecord)storage.read(toCheck.pop().intValue());
			if(shouldRun(record)) {
				PeriodicEvent e = d.get(record.getEventKey());
				e.init(this, record);
				J2MEDisplay.startStateWithLoadingScreen(e);
			} else {
				next();
			}
		}
	}
	
	private final boolean shouldRun(PeriodicEventRecord record) {
		//Check for weird conditions first
		
		return record.getNextTrigger().getTime() != 0 && record.getNextTrigger().getTime() < now.getTime();
	}
	
	public abstract void done(); 

}
