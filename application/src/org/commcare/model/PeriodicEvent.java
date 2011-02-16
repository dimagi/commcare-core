/**
 * 
 */
package org.commcare.model;

import java.util.Date;
import java.util.NoSuchElementException;

import org.javarosa.core.api.State;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;

/**
 * @author ctsims
 *
 */
public abstract class PeriodicEvent implements State {
	
	private PeriodicWrapperState parent;
	private PeriodicEventRecord r;
	
	public static final int TYPE_WEEKLY = 0;
	public static final int TYPE_DAILY = 1;
	public static final int TYPE_LOGIN = 2;
	
	
	public static final int TYPE_CUSTOM = 4;
	
	/** Events which are called upon on demand, rather than with set frequency **/
	public static final int TYPE_SCHEDULED= 8;

	// re-wrapped to prevent nokia bug. Do not remove.
	public abstract void start();
	
	protected abstract String getEventKey();
	protected abstract int getEventPeriod();

	protected void done() {
		parent.returnFromEvent(this, r);
	}

	public final PeriodicEventRecord scheduleNextTrigger(PeriodicEventRecord record) {
		Date nextSchedule = new Date();
		
		int type = this.getEventPeriod();
		
		if((type & TYPE_SCHEDULED)  == TYPE_SCHEDULED) {
			//Nothing, we need to wait for a bump
			nextSchedule = new Date(0);
		} else {
			nextSchedule = next(record,nextSchedule);
		}
		record.setLastOccurance(new Date());
		record.setNextScheduledDate(nextSchedule);
		return record;
	}
	
	private Date next(PeriodicEventRecord record, Date from) {
		if(record.getLastOccurance().getTime() == 0) { 
			return new Date();
		}
		
		//Strip off scheduled flag
		switch(this.getEventPeriod() ^ TYPE_SCHEDULED) {
			case TYPE_CUSTOM:
				return customScheduleNextTrigger(record, from);
			case TYPE_WEEKLY:
				return DateUtils.dateAdd(from, 7);
			case TYPE_DAILY:
				return DateUtils.dateAdd(from, 1);
			case TYPE_LOGIN:
				return new Date();
		}
		return new Date(0);
	}
	
	protected Date customScheduleNextTrigger(PeriodicEventRecord record, Date from) {
		throw new RuntimeException("Periodic events which are custom must implement the custom schedule function");
	}


	protected final void init(PeriodicWrapperState parent, PeriodicEventRecord record) {
		this.parent = parent;
		this.r = record;
	}
	

	//Should probably be in a helper elsewhere.
	public static void schedule(PeriodicEvent event) {
		
		if((event.getEventPeriod() & TYPE_SCHEDULED)  != TYPE_SCHEDULED) {
			//Hm, skip this if it's not a schedulable event?
		}
		
		String key = event.getEventKey();

		PeriodicEventRecord record = null;
		IStorageUtilityIndexed storage = (IStorageUtilityIndexed)StorageManager.getStorage(PeriodicEventRecord.STORAGE_KEY);
		
		try {
			record = (PeriodicEventRecord)storage.getRecordForValue(PeriodicEventRecord.META_EVENT_KEY, key);
		} catch(NoSuchElementException nsee) {
			//No big deal, no record;
		}
		
		//Never scheduled, create a new event with the right scheduling
		if(record == null) {
			//Next firing period should be immediate in this case.
			record = new PeriodicEventRecord(event.getEventKey(), new Date());
			try {
				storage.write(record);
			} catch (StorageFullException e) {
				Logger.exception("scheduling event", e);
			}
			return;
		}
		
		//Otherwise, there's already a record, we should see if the next expiration is earlier than any scheduled ones
		long current = record.getNextTrigger().getTime();
		Date last = record.getLastOccurance();
		
		Date next = event.next(record, last);
		
		if(current == 0 || current > next.getTime() ) {
			record.setNextScheduledDate(next);
			try {
				storage.write(record);
			} catch (StorageFullException e) {
				Logger.exception("scheduling event", e);
			}
		} 

		return;
	}
}
