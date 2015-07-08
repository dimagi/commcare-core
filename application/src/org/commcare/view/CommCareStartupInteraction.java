/**
 *
 */
package org.commcare.view;

import org.commcare.util.YesNoListener;
import org.javarosa.core.services.locale.Localization;

import de.enough.polish.ui.Command;
import de.enough.polish.ui.CommandListener;
import de.enough.polish.ui.Displayable;
import de.enough.polish.ui.Form;
import de.enough.polish.ui.Gauge;
import de.enough.polish.ui.StringItem;

/**
 * @author ctsims
 *
 */
public class CommCareStartupInteraction extends Form implements CommandListener {
    private StringItem messageItem;

    private YesNoListener listener;

    private Command yes;
    private Command no;

    private Command cancel;

    private Gauge gauge;

    public CommCareStartupInteraction(String message) {
        this(message, false);
    }

    public CommCareStartupInteraction(String message, boolean cancelable) {
        super(failSafeText("intro.title", "CommCare"));
        messageItem = new StringItem(null, null);
        //#style focused
        gauge = new Gauge(null, false, 100,0);
        this.append(messageItem);
        this.append(gauge);
        gauge.setVisible(false);
        this.setCommandListener(this);
        setMessage(message, true);
    }

    public void setMessage(String message){
        setMessage(message, false);
    }

    public void setMessage(String message, boolean showSpinner) {
        this.messageItem.setText(message);
        gauge.setVisible(showSpinner);
    }

    public void AskYesNo(String message, YesNoListener listener) {
        setMessage(message, false);
        this.listener = listener;
        if(yes == null){
            yes = new Command(failSafeText("yes","Yes"), Command.OK, 0);
            no = new Command(failSafeText("no","No"), Command.CANCEL, 0);

            this.addCommand(yes);
            this.addCommand(no);
        }
        else if(yes.getLabel() != "Yes" || no.getLabel() != "No"){
            yes.setLabel("Yes");
            no.setLabel("No");
        }
    }

    public void AskYesNo(String message, YesNoListener listener, String left, String right){
        setMessage(message, false);
        this.listener = listener;
        if(yes == null) {

            yes = new Command(left, Command.OK, 0);
            no = new Command(right, Command.CANCEL, 0);

            this.addCommand(yes);
            this.addCommand(no);
        }
        else{
            yes.setLabel(left);
            no.setLabel(right);
        }
    }

    public void PromptResponse(String message, YesNoListener listener) {
        setMessage(message, false);
        this.listener = listener;
        if(yes == null) {
            yes = new Command(failSafeText("ok","OK"), Command.OK, 0);

            this.addCommand(yes);
        }
    }

    public static String failSafeText(String localeId, String fallback) {
        try {
            return Localization.get(localeId);
        } catch(Exception e) {
            return fallback;
        }
    }

    public static String failSafeText(String localeId, String fallBack, String[] args){
        try {
            return Localization.get(localeId,args);
        } catch(Exception e) {
            return fallBack;
        }
    }

    public void commandAction(Command c, Displayable d) {
        if(c.equals(yes)) {
            listener.yes();
        } else if(c.equals(no)) {
            listener.no();
        }
        clearCommands();
    }

    public void updateProgess(int progress) {
        this.gauge.setValue(progress);
    }

    private void clearCommands() {
        this.removeCommand(yes);
        yes = null;
        this.removeCommand(no);
        no = null;
    }
}
