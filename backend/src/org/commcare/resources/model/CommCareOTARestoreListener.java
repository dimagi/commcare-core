package org.commcare.resources.model;

public interface CommCareOTARestoreListener {

    int REGULAR_START = 0;
    int BYPASS_START = 1;
    int BYPASS_CLEAN = 2;
    int BYPASS_CLEAN_SUCCESS = 3;
    int BYPASS_CLEANFAIL = 4;
    int BYPASS_FAIL = 5;
    int RESTORE_BAD_CREDENTIALS = 6;
    int RESTORE_CONNECTION_FAILED = 7;
    int RESTORE_BAD_DB = 8;
    int RESTORE_DB_BUSY = 9;
    int RESTORE_CONNECTION_MADE = 10;
    int RESTORE_BAD_DOWNLOAD = 11;
    int RESTORE_BAD_SERVER = 12;
    int RESTORE_FAIL_OTHER = 13;
    int RESTORE_DOWNLOAD = 14;
    int RESTORE_RECOVER_SEND = 15;
    int RESTORE_NO_CACHE = 16;
    int RESTORE_DOWNLOADED = 17;
    int RESTORE_NEED_CACHE = 18;
    int RESTORE_START = 19;
    int RESTORE_RECOVERY_WIPE = 20;
    int RESTORE_SUCCESS = 21;
    int RESTORE_FAIL = 22;
    int RESTORE_FAIL_PARTIAL = 23;
    int RESTORE_CONNECTION_FAIL_ENTRY = 24;

    /**
     * Called by the parseBlock method every time the restore task successfully
     * parses a new block of the restore form
     */
    void onUpdate(int numberCompleted);

    /**
     * Called when the parser encounters the "<items>" property in the restore file
     * Enables the progress bar
     */
    void setTotalForms(int totalItemCount);

    /**
     * Called whenever the restore task wants to update the view with information about
     * its current status
     *
     * @param a number, mapped above, corresponding to the current status of the restore
     */
    void statusUpdate(int statusNumber);

    /**
     * Called when the restore task wants to refresh the main view. Perhaps should be removed?
     * Seems like something that should be in the controller
     */
    void refreshView();


    //NOTE: the three below are all of the ways that the restorer will exit. You can block
    //the ui from user input until one is called

    /**
     * Called when the restore task wants to prompt the user for credentials
     */
    void getCredentials();

    /**
     * Called when the restore has failed but wants to give the user a chance to retry
     *
     * @param msg: the reason for the failure
     */
    void promptRetry(String msg);

    /**
     * Called when the asynchronous restore operation completes successfully
     */
    void onSuccess();

    /**
     * Called when the restore encounters an error that it wants to terminate
     * operation for, without a possibility of retrying
     *
     * @param failMessage - the failure message
     */
    void onFailure(String failMessage);

}
