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
 * A periodic event is an action that should be called up automatically
 * according to some sort of schedule. Periodic events are states which
 * can have an arbitrary interaction with the user, although they are
 * expected to only have one exit point.
 *
 * When implementing a PeriodicEvent, your event should specify 3 things at a minimum.
 *
 * 1) Event Period - How often the event can happen. Weekly, daily, or on every login.
 * In addition, if your event should only occur after it has been scheduled explicitly,
 * you should add that flag as a mask. IE: TYPE_DAILY & TYPE_FLAG_SCHEDULED.
 *
 * Scheduled events will occur at the soonest login after they are scheduled and after
 * the last scheduled period. So if a daily event has occurred within the last day, and is
 * scheduled, it will occur on the first login that is a day after the previous event.
 *
 * 2) The action of the event. You can implement this simply as the body of the event's state
 * as you would normally. When your event has finished, remember to trigger the done transition.
 *
 * 3) A unique Event Key that is used as an index for the actual event record. Any unique string
 * is appropriate (fully qualified class name is an entirely reasonable choice).
 *
 * The current method of scheduling events relies upon being able to create an event in a manner
 * distinct from executing that event, so it is advised that an event's constructor be empty and
 * that initialization not be required.
 *
 * @author ctsims
 *
 */
public abstract class PeriodicEvent implements State {

    private PeriodicWrapperState parent;
    private PeriodicEventRecord r;

    /** Event will occur at most weekly */
    public static final int TYPE_WEEKLY = 0;

    /** Event will occur at most daily */
    public static final int TYPE_DAILY = 1;

    /** Event will occur at most upon every login */
    public static final int TYPE_LOGIN = 2;

    /** Event will specify its own period */
    public static final int TYPE_CUSTOM = 4;

    /** Event will specify its own period */
    public static final int TYPE_DISABLED = 8;

    //NOTE: If a new event period is added, the PeriodicWrapperState's shouldRun
    //method needs to be updated to modify behavior around edge cases

    /** Events which are called upon on demand, rather than with set frequency **/
    public static final int TYPE_FLAG_SCHEDULED= 64;

    public static final int FLAGS = TYPE_FLAG_SCHEDULED;

    // re-wrapped to prevent nokia bug. Do not remove.
    public abstract void start();

    /**
     * @return A unique string which differentiates this event
     */
    protected abstract String getEventKey();

    /**
     * @return One of PeriodicEvent.TYPE_FOO. Apply the scheduled mask to require the event
     * to be explicitly scheduled.
     */
    protected abstract int getEventPeriod();

    protected void done() {
        parent.returnFromEvent(this, r);
    }

    /**
     * Provide a record for when the next event should occur based on the last occurrence of this
     * event.
     *
     * @param record The last occurrence of this event.
     * @return A record which specifies when this event should next be triggered.
     */
    public final PeriodicEventRecord scheduleNextTrigger(PeriodicEventRecord record) {
        Date nextSchedule = new Date();

        int type = this.getEventPeriod();

        if((type & TYPE_FLAG_SCHEDULED)  == TYPE_FLAG_SCHEDULED) {
            //Nothing, we need to wait for a bump
            nextSchedule = new Date(0);
        } else {
            nextSchedule = next(record,nextSchedule);
        }
        record.setLastOccurance(new Date());
        record.setNextScheduledDate(nextSchedule);
        return record;
    }

    /**
     * Provided the actual date that the next event should occur. Does
     * not care about scheduling.
     *
     * @param record The last available record.
     * @param from The date which should be assumed to be the previous occurrence
     * @return The date at which the next triggering should occur
     */
    private Date next(PeriodicEventRecord record, Date from) {
        if(from.getTime() == 0) {
            return new Date();
        }

        //Strip off scheduled flag
        switch(this.getEventPeriod() & ~TYPE_FLAG_SCHEDULED) {
            case TYPE_CUSTOM:
                return customScheduleNextTrigger(record, from);
            case TYPE_WEEKLY:
                return DateUtils.dateAdd(from, 7);
            case TYPE_DAILY:
                return DateUtils.dateAdd(from, 1);
            case TYPE_LOGIN:
                return new Date();
            case TYPE_DISABLED:
                return new Date(0);
        }
        return new Date(0);
    }

    /**
     * If this event's Period is TYPE_CUSTOM, this method must be overridden to provide the next trigger
     * period.
     *
     * @param record The last available record (if any) of this event's triggering
     * @param from The last time when this event should be assumed to have occurred.
     * @return The next date at which this event should be triggered.
     */
    protected Date customScheduleNextTrigger(PeriodicEventRecord record, Date from) {
        throw new RuntimeException("Periodic events which are custom must implement the custom schedule function");
    }


    protected final void init(PeriodicWrapperState parent, PeriodicEventRecord record) {
        this.parent = parent;
        this.r = record;
    }


    /**
     * The logic for scheduling a specific type of periodic event. After this method is called,
     * the event will be fired at the next possible opportunity based on the event's scheduling and
     * previous execution.
     *
     * @param event A specific event to schedule.
     */
    public static void schedule(PeriodicEvent event) {
        //This method should probably be in a helper elsewhere.

        if((event.getEventPeriod() & TYPE_FLAG_SCHEDULED)  != TYPE_FLAG_SCHEDULED) {
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
            if(event.getEventPeriod() == PeriodicEvent.TYPE_DISABLED) {
                record = new PeriodicEventRecord(event.getEventKey(), new Date(0));
            } else {
                record = new PeriodicEventRecord(event.getEventKey(), new Date());
            }
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

    public static void markTriggered(PeriodicEvent event) {
        //For events which can be triggered manually (outside of the framework), and want to have their occurances updated.
        PeriodicEventRecord record = null;
        IStorageUtilityIndexed storage = (IStorageUtilityIndexed)StorageManager.getStorage(PeriodicEventRecord.STORAGE_KEY);

        try {
            record = (PeriodicEventRecord)storage.getRecordForValue(PeriodicEventRecord.META_EVENT_KEY, event.getEventKey());
        } catch(NoSuchElementException nsee) {
            //No record. Any scheduled events would have one, so don't sweat it and just get outta here.
            System.out.println("No initialized records of type: " + event.getEventKey() + ". Skipping scheduling");
            return;
        }

        PeriodicEventRecord nextTrigger = event.scheduleNextTrigger(record);
        try {
            storage.write(nextTrigger);
            System.out.println("Event[" + event.getEventKey() + "] manually triggered. Next Execution due at " + DateUtils.formatDate(nextTrigger.getNextTrigger(), DateUtils.FORMAT_HUMAN_READABLE_SHORT));
        } catch (StorageFullException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to update periodic event record storage");
        }
    }
}
