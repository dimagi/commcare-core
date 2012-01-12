/**
 * 
 */
package org.javarosa.j2me.view;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;

import org.javarosa.core.services.locale.Localization;

/**
 * @author ctsims
 *
 */
public class LoadingScreen extends Form {
	
	private final static int RESOLUTION = 100;
	
	private Gauge gauge;
	
	public LoadingScreen(ProgressIndicator indicator) {
		super(Localization.get("loading.screen.title"));
		String message = Localization.get("loading.screen.message");
		if(indicator != null && (indicator.getIndicatorsProvided() & ProgressIndicator.INDICATOR_STATUS) != 0) {
			message = indicator.getCurrentLoadingStatus();
		}
		
		if(indicator != null && (indicator.getIndicatorsProvided() & ProgressIndicator.INDICATOR_PROGRESS) != 0) {
			gauge = new Gauge(message, true, RESOLUTION, 0);
		} else{
			//#style loadingGauge?
			gauge = new Gauge(message, false, Gauge.INDEFINITE,Gauge.CONTINUOUS_RUNNING);
		}
		this.append(gauge);
	}

	public void updateProgress(double progress) {
		gauge.setValue((int)Math.floor(RESOLUTION*progress));
	}
	
	public void updateMessage(String message) {
		gauge.setLabel(message);
	}

}
