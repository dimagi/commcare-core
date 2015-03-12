/**
 *
 */
package org.commcare.restore;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Gauge;

import org.javarosa.core.services.locale.Localization;

import de.enough.polish.ui.Form;
import de.enough.polish.ui.StringItem;
import de.enough.polish.ui.UiAccess;

/**
 * @author ctsims
 *
 */
public class CommCareOTAFailView extends Form{

    public static final Command DOWNLOAD = new Command(Localization.get("restore.retry"), Command.OK, 1);
    public static final Command CANCEL = new Command(Localization.get("polish.command.cancel"), Command.BACK, 2);

    private StringItem fetch;

    private final static int RESOLUTION = 100;

    StringItem details;
    String buffer;

    StringItem failTitle;
    String failMessage = Localization.get("restore.fail.view");

    int count = 0;

    boolean finished;
    boolean gaugeIsInfinite = true;
    int totalItems;

    public CommCareOTAFailView(String title) {
        super(title);
        details = new StringItem("","");

        failTitle = new StringItem(failMessage,"");
        this.append(failTitle);

        this.append(details);
        buffer = "";

        this.addCommand(DOWNLOAD);
        this.addCommand(CANCEL);
    }

    public void addToMessage(String message) {
        buffer = message + "\n" + buffer;
        setMessage(buffer);
    }

    public void setMessage(String message) {
        buffer = message;
        details.setText(buffer);
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
                    UiAccess.cast(this.getCommandListener()).commandAction(CANCEL,this);
                    return true;
                }
            }
            return false;
        }
    }

    public void setInteractive(boolean isInteractive) {
        if(isInteractive) {
            this.addCommand(CANCEL);
            this.addCommand(DOWNLOAD);
        } else {
            this.removeCommand(CANCEL);
            this.removeCommand(DOWNLOAD);
        }
    }
}
