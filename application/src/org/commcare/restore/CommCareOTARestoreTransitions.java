package org.commcare.restore;

public interface CommCareOTARestoreTransitions {
    public void cancel();
    public void done(boolean errorsOccurred);
    public void commitSyncToken(String restoreID);
}
