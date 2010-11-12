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
	
	//One hour since last send-all-unsent attempt
	private static final long FAILURE_REST_INTERVAL = 1000 * 60 * 60 * 1; 
	
	//Every 2 Minutes
	private static final long TIMER_PERIOD = 1000 * 60 * 2;
	
	//One minute
	private static final long TIMER_FIRST = 1000 * 60;
	
	private static long nextValidAttempt = 0;
	
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
			
			//Start sending data in this thread
			try {
				listener.reinit();
				//This isn't in a thread, so we know that we're done whenever it finishes.
				TransportService.sendCached(listener);
				
				if(listener.failed()) {
					//If we failed to send successfully, wait a bit before trying again.
					nextValidAttempt += new Date().getTime() + FAILURE_REST_INTERVAL;
				}
				
				listener.expire();
			} catch (TransportException e) {
				e.printStackTrace();
				Logger.exception("Send all unsent auto-failure", e);
				
				//If we failed to send successfully, wait a bit before trying again.
				nextValidAttempt += new Date().getTime() + FAILURE_REST_INTERVAL;
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
			}, TIMER_FIRST, TIMER_PERIOD);
		}
	}
	
	/**
	 * Notify the service that a new element is on the queue and ready to be sent.
	 * Overrides any delays from previous failures.
	 */
	public static void NotifyPending() {
		synchronized(lock) {
			nextValidAttempt = 0;
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
}
