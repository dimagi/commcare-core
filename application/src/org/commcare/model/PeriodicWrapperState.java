/**
 *
 */
package org.commcare.model;

import org.javarosa.core.api.State;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.DataUtil;
import org.javarosa.j2me.view.J2MEDisplay;

import java.util.Date;
import java.util.Hashtable;
import java.util.Stack;

/**
 * The periodic wrapper state is responsible for handling the execution of scheduled
 * periodic events. It identifies what events are ready to be triggered, executes them
 * one-by-one, and returns after all events have been executed and rescheduled if necessary.
 *
 * It requires a list of all of the periodic events currently defined, which should be available
 * from the application's context.
 *
 * @author ctsims
 *
 */
public abstract class PeriodicWrapperState implements State {

    private Hashtable<String, PeriodicEvent> d;
    private Stack<Integer> toCheck;
    private Date now;

    IStorageUtility storage;

    /**
     * Creates a wrapper state ready for execution.
     *
     * @param descriptors All of the periodic events defined in the
     * current environment.
     */
    public PeriodicWrapperState(PeriodicEvent[] descriptors) {

        now = new Date();

        d = new Hashtable<String, PeriodicEvent>();
        for(PeriodicEvent e : descriptors) {
            d.put(e.getEventKey(), e);
        }

        storage = StorageManager.getStorage(PeriodicEventRecord.STORAGE_KEY);
        toCheck = new Stack<Integer>();
        for(IStorageIterator i = storage.iterate(); i.hasMore(); ) {
            toCheck.addElement(DataUtil.integer(i.nextID()));
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.api.State#start()
     */
    public void start() {
        next();
    }

    /**
     * Called by a periodic event after execution. Schedules the next event trigger.
     *
     * @param e The event which has executed.
     * @param record The record which led to executing the event.
     */
    protected void returnFromEvent(PeriodicEvent e, PeriodicEventRecord record) {
        PeriodicEventRecord nextEvent = e.scheduleNextTrigger(record);

        if(nextEvent == null) {
            //TOOD: This doesn't really make sense, since it prevents properly scheduling
            //future events. Possibly a good trap door, though?
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

    /**
     * Executes the next available event (if any) or returns if done.
     */
    private void next(){
        if(toCheck.empty()) {
            done();
        } else {
            PeriodicEventRecord record = (PeriodicEventRecord)storage.read(toCheck.pop().intValue());
            PeriodicEvent e = d.get(record.getEventKey());
            if(shouldRun(record, e)) {
                e.init(this, record);
                J2MEDisplay.startStateWithLoadingScreen(e);
            } else {
                next();
            }
        }
    }

    /**
     * Identifies whether the record implies that the event should be run. This will be the case not
     * only if the execution time is ready, but also if certain edge cases are detected like the time of
     * execution being radically different from expected ranges (due to the phone's date/time changing externally).
     *
     * @param record The record of a scheduled event
     * @return True if that event should be fired. False otherwise.
     */
    private final boolean shouldRun(PeriodicEventRecord record, PeriodicEvent event) {

        long next = record.getNextTrigger().getTime();

        //check unscheduled first
        if(next == 0) { return false; }

        //TODO: Identify other boundary conditions

        //abs(Next Execution - today) > max event period + buffer

        //The span at which something fishy is probably going on
        //default to 1 day longer than the longest day
        long difference = DateUtils.DAY_IN_MS * 8;

        //TODO: Replace this handling of edge cases in the event itself
        //with a (Defaulted) "scheduleDisrupted" thing. much cleaner

        if(((event.getEventPeriod() ^ PeriodicEvent.FLAGS) == PeriodicEvent.TYPE_CUSTOM)) {
            //Custom event span. Tricky.

            //TODO: We could technically make this twice the previous custom period or something,
            //but that kind of defeats the point? For now we'll skip worrying about this.
        } else {
            if(Math.abs(record.getNextTrigger().getTime() - now.getTime()) > difference) {
                //Assume time was changed.
                Logger.log("periodic_e", "Time change detected. Next execution: " +
                                         DateUtils.formatDateToTimeStamp(record.getNextTrigger()) +
                                         "Current time " +
                                         DateUtils.formatDateToTimeStamp(now));
                return true;
            }
        }

        //Normal trigger condition
        return record.getNextTrigger().getTime() < now.getTime();
    }

    public abstract void done();

}
