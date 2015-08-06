/**
 *
 */
package org.commcare.restore;

import org.commcare.core.properties.CommCareProperties;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.user.api.CreateUserController;

import javax.microedition.lcdui.Command;

import de.enough.polish.ui.Form;
import de.enough.polish.ui.StringItem;
import de.enough.polish.ui.TextField;

/**
 * @author ctsims
 *
 */
public class CommCareOTACredentialEntry extends Form {

    public static final Command DOWNLOAD = new Command(Localization.get("restore.fetch"), Command.OK, 1);
    public static final Command CANCEL = new Command(Localization.get("polish.command.cancel"), Command.BACK, 2);

    private StringItem fetch;

    private StringItem instructions;
    private StringItem updates;
    private TextField username;
    private TextField password;

    public CommCareOTACredentialEntry(String title) {
        super(title);

        instructions = new StringItem("",Localization.get("restore.login.instructions"));
        this.append(instructions);

        username = new TextField(Localization.get("form.login.username"), null, 50, TextField.ANY);

        String passFormat = PropertyManager._().getSingularProperty(CommCareProperties.PASSWORD_FORMAT);

        int flags = CreateUserController.PASSWORD_FORMAT_ALPHA_NUMERIC.equals(passFormat) ? TextField.PASSWORD : TextField.PASSWORD | TextField.NUMERIC;

        password = new TextField(Localization.get("form.login.password"), null, 100, flags);

        this.append(username);
        this.append(password);

        //#style button
        fetch = new StringItem("",Localization.get("restore.fetch"));
        fetch.setDefaultCommand(DOWNLOAD);
        this.append(fetch);
        this.addCommand(CANCEL);

        updates = new StringItem("","");
        this.append(updates);
        setInteractive(true);
    }

    public String getUsername() {
        return username.getString();
    }

    public String getPassword() {
        return password.getString();
    }

    public void sendMessage(String message) {
        this.updates.setText(message);
    }

    public void setInteractive(boolean isInteractive) {
        if(isInteractive) {
            this.addCommand(CANCEL);
            fetch.setVisible(true);
        } else {
            this.removeCommand(CANCEL);
            fetch.setVisible(false);
        }
    }

    public void setUsername(String sampleUsername) {
        username.setText(sampleUsername);
    }
}
