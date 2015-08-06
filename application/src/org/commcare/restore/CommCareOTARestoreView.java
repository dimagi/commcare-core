/**
 *
 */
package org.commcare.restore;

import org.javarosa.core.services.locale.Localization;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Gauge;

import de.enough.polish.ui.Form;
import de.enough.polish.ui.StringItem;
import de.enough.polish.ui.UiAccess;

/**
 * @author ctsims
 *
 */
public class CommCareOTARestoreView extends Form{

    public final Command FINISHED = new Command(Localization.get("restore.finished"),Command.SCREEN,1);

    private Gauge gauge;
    private final static int RESOLUTION = 100;

    StringItem details;
    String buffer;

    int count = 0;

    boolean finished;
    boolean gaugeIsInfinite = true;
    int totalItems;

    public CommCareOTARestoreView(String title) {
        super(title);
        details = new StringItem("","");

        gauge = new Gauge(title, false, Gauge.INDEFINITE,Gauge.CONTINUOUS_RUNNING);
        this.append(gauge);

        this.append(details);
        buffer = "";
    }

    public void addToMessage(String message) {
        //buffer = message + "\n" + buffer;
        buffer = message;
        setMessage(buffer);
    }

    public void setMessage(String message) {
        buffer = message;
        details.setText(buffer);
    }

    public void setTotalItems(int totalItems){
        this.totalItems = totalItems;
        gaugeIsInfinite = false;
        gauge = new Gauge(getTitle(), true, RESOLUTION, 0);
        this.deleteAll();
        this.append(gauge);
        this.append(details);
    }

    public void updateProgress(int finishedItems) {
        if(gaugeIsInfinite){
            addToMessage(Localization.get("restore.ui.unbounded", new String [] {""+finishedItems}));
        }
        else{
            gauge.setValue((int)Math.floor(RESOLUTION*((finishedItems*1.0)/totalItems)));
            addToMessage(Localization.get("restore.ui.bounded", new String [] {""+finishedItems,""+totalItems}));
        }
    }

    public void setFinished() {
        this.addCommand(FINISHED);
        //Scroll to the bottom
        this.setScrollYOffset(this.getScreenFullHeight() - this.contentHeight, true);
        this.finished = true;
    }

    protected boolean handleKeyReleased(int keyCode, int gameAction) {
        if(super.handleKeyReleased(keyCode, gameAction)) {
            //Don't do anything that already does something.
            return true;
        } else {
            if(finished) {
                //scrolling should be uninterrupted
                switch(gameAction) {
                case Canvas.UP:
                case Canvas.DOWN:
                case Canvas.LEFT:
                case Canvas.RIGHT:
                    return false;
                default:
                    UiAccess.cast(this.getCommandListener()).commandAction(FINISHED,this);
                    return true;
                }
            }
            return false;
        }
    }
    public void stopGauge(){
        this.deleteAll();
        this.append(details);

    }
}
