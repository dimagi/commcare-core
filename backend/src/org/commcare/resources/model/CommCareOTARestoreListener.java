package org.commcare.resources.model;

public abstract interface CommCareOTARestoreListener {

		public final int REGULAR_START = 123;
	
		public final int BYPASS_START = 0;
		public final int BYPASS_CLEAN = 1;
		public final int BYPASS_CLEAN_SUCCESS = 2;
		public final int BYPASS_CLEANFAIL = 4;
		public final int BYPASS_FAIL = 8;
		
		public final int RESTORE_BAD_CREDENTIALS = 16;
		public final int RESTORE_CONNECTION_FAILED = 32;
		public final int RESTORE_BAD_DB = 64;
		public final int RESTORE_DB_BUSY = 128;
		public final int RESTORE_CONNECTION_MADE = 256;
		public final int RESTORE_BAD_DOWNLOAD = 512;
		public final int RESTORE_BAD_SERVER = 1024;
		public final int RESTORE_FAIL_OTHER = 2048;
		public final int RESTORE_DOWNLOAD = 5096;
		public final int RESTORE_RECOVER_SEND = 10192;
		
		public final int RESTORE_NO_CACHE = 3;
		public final int RESTORE_DOWNLOADED = 9;
		public final int RESTORE_NEED_CACHE = 27;
		public final int RESTORE_START = 81;
		public final int RESTORE_RECOVERY_WIPE = 243;
		
		public final int RESTORE_SUCCESS = 5;
		public final int RESTORE_FAIL = 25;
		public final int RESTORE_FAIL_PARTIAL = 125;
		public final int RESTORE_CONNECTION_FAIL_ENTRY = 625;
		
		public abstract void onSuccess();

		public abstract void onFailure(String failMessage);
		
		public abstract void onUpdate(int numberCompleted);
		
		public abstract void setTotalForms(int totalItemCount);
		
		public abstract void statusUpdate(int statusNumber);
		
		public abstract void refreshView();
		
		public abstract void getCredentials();

	}
