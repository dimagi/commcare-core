/**
 * 
 */
package org.commcare.core.backup;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

/**
 * @author ctsims
 *
 */
public class BackupRestoreStatusView extends Form {
	
	StringItem svStatus;

	
	public BackupRestoreStatusView() {
		super("Please Wait...");
		svStatus = new StringItem("", "");
		this.append(svStatus);
	}
	
	public void updateMessage(String message) {
		this.svStatus.setText(message);
	}
}
