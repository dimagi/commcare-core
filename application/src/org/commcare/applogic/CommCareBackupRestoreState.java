package org.commcare.applogic;

import org.commcare.core.properties.CommCareProperties;
import org.javarosa.core.services.PropertyManager;

public abstract class CommCareBackupRestoreState extends BackupRestoreState {
	public CommCareBackupRestoreState (Class backuprestoreimage) {
		super(backuprestoreimage,
			  PropertyManager._().getSingularProperty(CommCareProperties.BACKUP_MODE),
			  PropertyManager._().getSingularProperty(CommCareProperties.BACKUP_URL),
			  PropertyManager._().getSingularProperty(CommCareProperties.RESTORE_URL));
	}
}
