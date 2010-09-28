/**
 * 
 */
package org.commcare.restore;

import javax.microedition.lcdui.Command;

import org.commcare.core.properties.CommCareProperties;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.user.api.AddUserController;
import org.javarosa.user.api.LoginController;

import de.enough.polish.ui.Form;
import de.enough.polish.ui.StringItem;
import de.enough.polish.ui.TextField;

/**
 * @author ctsims
 *
 */
public class CommCareOTACredentialEntry extends Form {
	
	public static final Command DOWNLOAD = new Command(Localization.get("restore.fetch"), Command.OK, 1);
	public static final Command CANCEL = new Command(Localization.get("polish.command.cancel"), Command.CANCEL, 1);
	
	private StringItem fetch;
	
	private StringItem instructions;
	private StringItem updates;
	private TextField username;
	private TextField password;

	public CommCareOTACredentialEntry(String title) {
		super(title);
		this.addCommand(CANCEL);
		
		instructions = new StringItem("",Localization.get("restore.login.instructions"));
		this.append(instructions);
		
		username = new TextField(Localization.get("form.login.username"), null, 50, TextField.ANY);
		
		String passFormat = PropertyManager._().getSingularProperty(CommCareProperties.PASSWORD_FORMAT);
		
		int flags = AddUserController.PASSWORD_FORMAT_ALPHA_NUMERIC.equals(passFormat) ? TextField.PASSWORD : TextField.PASSWORD | TextField.NUMERIC;

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
}
