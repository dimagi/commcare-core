/**
 * 
 */
package org.commcare.services;

import java.util.Timer;
import java.util.TimerTask;

import org.javarosa.j2me.log.HandledTimerTask;
import org.javarosa.services.transport.TransportListener;
import org.javarosa.services.transport.TransportMessage;
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
	
	//Every 5 Minutes
	private static final int TIMER_PERIOD = 1000 * 60 * 5;
	
	//One minute
	private static final int TIMER_FIRST = 1000 * 60;
	
	SignalLevelProvider provider;
	
	TransportListener listener;
	
	private AutomatedSenderService(SignalLevelProvider provider) {
		this.provider = provider;
		
		listener = new TransportListener() {

			public void onChange(TransportMessage message, String remark) {
				//Not relevant.
			}

			public void onStatusChange(TransportMessage message) {
				//
			}
			
		};
	}
	
	private void timeout() {
		//Identify that no previous timeout is occurring.
		
		//Establish whether we should skip due to bad signal or too recent
		//send attempt
		if(provider == null) {
			
		} else {
			if(!provider.isDataPossible()) {
				return;
			}
		}
		//Establish whether there's anything to send
		if(TransportService.getCachedMessagesSize() < 1) {
			return;
		}
			
		//Start sending data (which thread?)
		try {
			TransportService.sendCached(listener);
		} catch (TransportException e) {
			e.printStackTrace();
			
			//LOG!!!!
			//NOTE: I don't think this can ever happen.
		}
	}
	
	/**
	 * The main entry point to the sender service. This should be called
	 * a maximum of one time per application instance and will instantiate
	 * and configure the sender service for the lifetime of the application.
	 */
	public static void InitializeAndSpawnSenderService() {
		//Establish whether a signal level provider is available
		service = new AutomatedSenderService(EstablishProvider());
		serviceTimer = new Timer();
		serviceTimer.schedule(new HandledTimerTask() {
			@Override
			public void _run() {
				service.timeout();
			}
		}, TIMER_FIRST, TIMER_PERIOD);
	}
	
	/**
	 * Terminate the sending service fully and release all resources. There is
	 * no guarantee that the service will be able to be restarted after this call
	 * has completed.
	 */
	public static void StopSenderService() {
		
	}
	
	private static SignalLevelProvider EstablishProvider() {
		return null;
	}
}
