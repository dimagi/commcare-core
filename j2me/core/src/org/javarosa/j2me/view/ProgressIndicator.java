/**
 *
 */
package org.javarosa.j2me.view;

/**
 * @author ctsims
 *
 */
public interface ProgressIndicator {

    public static final int INDICATOR_PROGRESS = 1;
    public static final int INDICATOR_STATUS = 2;

    public double getProgress();

    public String getCurrentLoadingStatus();

    public int getIndicatorsProvided();
}
