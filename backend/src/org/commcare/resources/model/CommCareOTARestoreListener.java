package org.commcare.resources.model;

public interface CommCareOTARestoreListener {

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
}
