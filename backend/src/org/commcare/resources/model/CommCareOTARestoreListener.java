package org.commcare.resources.model;

public abstract interface CommCareOTARestoreListener {

    public final int REGULAR_START = 0;
    public final int BYPASS_START = 1;
    public final int BYPASS_CLEAN = 2;
    public final int BYPASS_CLEAN_SUCCESS = 3;
    public final int BYPASS_CLEANFAIL = 4;
    public final int BYPASS_FAIL = 5;
    public final int RESTORE_BAD_CREDENTIALS = 6;
    public final int RESTORE_CONNECTION_FAILED = 7;
    public final int RESTORE_BAD_DB = 8;
    public final int RESTORE_DB_BUSY = 9;
    public final int RESTORE_CONNECTION_MADE = 10;
    public final int RESTORE_BAD_DOWNLOAD = 11;
    public final int RESTORE_BAD_SERVER = 12;
    public final int RESTORE_FAIL_OTHER = 13;
    public final int RESTORE_DOWNLOAD = 14;
    public final int RESTORE_RECOVER_SEND = 15;
    public final int RESTORE_NO_CACHE = 16;
    public final int RESTORE_DOWNLOADED = 17;
    public final int RESTORE_NEED_CACHE = 18;
    public final int RESTORE_START = 19;
    public final int RESTORE_RECOVERY_WIPE = 20;
    public final int RESTORE_SUCCESS = 21;
    public final int RESTORE_FAIL = 22;
    public final int RESTORE_FAIL_PARTIAL = 23;
    public final int RESTORE_CONNECTION_FAIL_ENTRY = 24;

    /**
     * Called by the parseBlock method every time the restore task successfully
     * parses a new block of the restore form
     *
     * @param numberCompleted
     */
    public abstract void onUpdate(int numberCompleted);

    /**
     * Called when the parser encounters the "<items>" property in the restore file
     * Enables the progress bar
     *
     * @param totalItemCount
     */
    public abstract void setTotalForms(int totalItemCount);

    /**
     * Called whenever the restore task wants to update the view with information about
     * its current status
     *
     * @param a number, mapped above, corresponding to the current status of the restore
     */
    public abstract void statusUpdate(int statusNumber);

    /**
     * Called when the restore task wants to refresh the main view. Perhaps should be removed?
     * Seems like something that should be in the controller
     */
    public abstract void refreshView();


    //NOTE: the three below are all of the ways that the restorer will exit. You can block
    //the ui from user input until one is called

    /**
     * Called when the restore task wants to prompt the user for credentials
     */
    public abstract void getCredentials();

    /**
     * Called when the restore has failed but wants to give the user a chance to retry
     *
     * @param msg: the reason for the failure
     */
    public abstract void promptRetry(String msg);

    /**
     * Called when the asynchronous restore operation completes successfully
     */
    public abstract void onSuccess();

    /**
     * Called when the restore encounters an error that it wants to terminate
     * operation for, without a possibility of retrying
     *
     * @param failMessage - the failure message
     */
    public abstract void onFailure(String failMessage);

}
