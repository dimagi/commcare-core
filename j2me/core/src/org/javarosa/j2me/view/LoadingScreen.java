/**
 *
 */
package org.javarosa.j2me.view;

import org.javarosa.core.services.locale.Localization;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;

/**
 * @author ctsims
 *
 */
public class LoadingScreen extends Form {

    private final static int RESOLUTION = 100;

    private Gauge gauge;

    public LoadingScreen(ProgressIndicator indicator) {
        this(indicator, Localization.get("loading.screen.title"), Localization.get("loading.screen.message"));
    }

    public LoadingScreen(ProgressIndicator indicator, String title, String message) {
        super(title);
        if(indicator != null && (indicator.getIndicatorsProvided() & ProgressIndicator.INDICATOR_STATUS) != 0) {
            message = indicator.getCurrentLoadingStatus();
        }

        if(indicator != null && (indicator.getIndicatorsProvided() & ProgressIndicator.INDICATOR_PROGRESS) != 0) {
            //#style focused
            gauge = new Gauge(message, false, RESOLUTION, 0);
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
