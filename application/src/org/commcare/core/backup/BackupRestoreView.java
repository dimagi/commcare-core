/**
 * 
 */
package org.commcare.core.backup;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import org.commcare.core.properties.CommCareProperties;

/**
 * @author ctsims
 *
 */
public class BackupRestoreView extends Form {
	private TextField avCode;
	
	private StringItem restoreButton;
	private StringItem backupButton;

	public BackupRestoreView(String mode) {
			super("Backup/Restore");
			avCode = new TextField("Restore Code:", "", 6, TextField.NUMERIC);
						
			backupButton = new StringItem("", "BACKUP", Item.BUTTON);
			restoreButton = new StringItem("", "RESTORE", Item.BUTTON);
			
	        if ( mode.compareTo(CommCareProperties.BACKUP_MODE_FILE)==0 ){
	            StringItem instructions = new StringItem("Insert a memory card before clicking BACKUP.",null);            
	            append(instructions);
	        }
			append(backupButton);
	        append(new StringItem("", ""));
	        if ( mode.compareTo(CommCareProperties.BACKUP_MODE_HTTP)==0 ){
	            append(new StringItem("", ""));
	            append(avCode);
	        }
			append(restoreButton);
	}
	
	public void setCommands(Command backupCommand, Command restoreCommand) {
		backupButton.setDefaultCommand(backupCommand);
		restoreButton.setDefaultCommand(restoreCommand);
	}
	
	public String getCode() {
		return avCode.getString();
	}
}
