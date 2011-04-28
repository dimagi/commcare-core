/**
 * 
 */
package org.commcare.services;

import java.util.Date;
import java.util.Timer;

import org.javarosa.core.services.Logger;
import org.javarosa.j2me.log.HandledTimerTask;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportException;

/**
 * The automated sender service is responsible for taking over the 
 * behaviors that would generally be controlled by a sync or send
 * unsent menu option. 
 * 
 * Depending on the phone's model, it may be capable of checking 
 * the phone's current signal level and attempting to send whenever 
 * convenient.
 * 
 * Otherwise the service will have an interval in which it attempts 
 * to handle sending, and will abort the process if it identifies that
 * it is not successful early on.
 * 
 * @author ctsims
 *
 */
public class AutomatedSenderService {
	
	private static Timer serviceTimer;
	private static AutomatedSenderService service;
	
	private static final String lock = "lock";
	
	private static final int POLL_INTERVAL = 20; //seconds
	
	private static final int[] BACKOFF_INTERVALS = {
		30,
		90,
		300,
		900,
		3600
	}; //seconds
	
	//TODO: should we store this in a property so that the back-off interval
	//persists across app restarts?
	private static long nextValidAttempt = 0;
	private static int curBackoffInterval = 0;
	
	SignalLevelProvider provider;
	
	AutomatedTransportListener listener;
	
	private AutomatedSenderService(SignalLevelProvider provider) {
		this.provider = provider;
		
		listener = new AutomatedTransportListener();
	}
	
	private void timeout() {
		synchronized(lock) {
			//Identify that no previous timeout is occurring (Shouldn't happen anyway, due to 
			//timer)
			if(listener.engaged()) {
				return;
			}
			
			//Establish whether we should skip due to bad signal or too recent
			//send attempt
			if(provider == null) {
				if(new Date().getTime() < nextValidAttempt ) {
					return;
				}
			} else {
				if(!provider.isDataPossible()) {
					return;
				}
			}
			//Establish whether there's anything to send
			if(TransportService.getCachedMessagesSize() < 1) {
				return;
			}
			
			if( nextValidAttempt != 0) {
				//Overcame a wait time, log this (to identify whether this ever comes up)
				Logger.log("auto-send", "Wait time expired. Re-trying send all unsent");
			}
			
			//Start sending data in this thread
			try {
				listener.reinit();
				//This isn't in a thread, so we know that we're done whenever it finishes.
				TransportService.sendCached(listener);
				
				if(listener.failed()) {
					//If we failed to send successfully, wait a bit before trying again.
					incrementDelay();
					Logger.log("auto-send", "Sender failed to submit data, suspending attempts for an hour");
				} else {
					//Clear any wait time
					resetDelay();
				}
				
				listener.expire();
			} catch (TransportException e) {
				e.printStackTrace();
				Logger.exception("Send all unsent auto-failure", e);
				
				//If we failed to send successfully, wait a bit before trying again.
				incrementDelay();
			}
		}
	}
	
	/**
	 * The main entry point to the sender service. This should be called
	 * a maximum of one time per application instance and will instantiate
	 * and configure the sender service for the lifetime of the application.
	 */
	public static void InitializeAndSpawnSenderService() {
		synchronized(lock) {
			//Establish whether a signal level provider is available
			service = new AutomatedSenderService(EstablishProvider());
			serviceTimer = new Timer();
			serviceTimer.schedule(new HandledTimerTask() {
				public void _run() {
					service.timeout();
				}
			}, POLL_INTERVAL * 1000, POLL_INTERVAL * 1000);
		}
	}
	
	/**
	 * Notify the service that a new element is on the queue and ready to be sent.
	 * Overrides any delays from previous failures.
	 */
	public static void NotifyPending() {
		synchronized(lock) {
			resetDelay();
		}
	}
	
	/**
	 * Terminate the sending service fully and release all resources. There is
	 * no guarantee that the service will be able to be restarted after this call
	 * has completed.
	 */
	public static void StopSenderService() {
		serviceTimer.cancel();
		TransportService.halt();
	}
	
	private static SignalLevelProvider EstablishProvider() {
		return null;
	}
	
	private static void resetDelay() {
		nextValidAttempt = 0;
		curBackoffInterval = 0;
	}
	
	private static void incrementDelay() {
		nextValidAttempt = new Date().getTime() + 1000 * BACKOFF_INTERVALS[curBackoffInterval];
		if (curBackoffInterval < BACKOFF_INTERVALS.length - 1) {
			curBackoffInterval++;
		}
	}
}
