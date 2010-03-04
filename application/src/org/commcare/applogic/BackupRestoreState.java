/**
 * 
 */
package org.commcare.applogic;

import org.commcare.core.backup.BackupRestoreController;
import org.commcare.core.backup.BackupRestoreStatusView;
import org.commcare.core.backup.BackupRestoreView;
import org.javarosa.core.api.State;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.j2me.view.J2MEDisplay;

/**
 * @author ctsims
 *
 */
public abstract class BackupRestoreState implements State, TrivialTransitions {

	private BackupRestoreController controller;
	private BackupRestoreView view;
	
	public BackupRestoreState(Class snapshotClass, String backupMode, String backupUrl, String restoreUrl) {
		view = new BackupRestoreView(backupMode);
        controller = new BackupRestoreController(
        		snapshotClass,
        		backupUrl,
        		restoreUrl,
        		backupMode,
        		view,
        		new BackupRestoreStatusView(),
        		this);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.api.State#start()
	 */
	public void start() {
		J2MEDisplay.setView(view);
	}

}
