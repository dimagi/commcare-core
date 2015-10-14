/**
 *
 */
package org.commcare.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Hashtable;

import org.commcare.cases.model.Case;
import org.commcare.cases.util.CaseDBUtils;
import org.commcare.core.properties.CommCareProperties;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.model.PeriodicEvent;
import org.commcare.resources.model.CommCareOTARestoreListener;
import org.commcare.restore.CommCareOTARestoreTransitions;
import org.commcare.util.time.PermissionsEvent;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.io.StreamsUtil.InputIOException;
import org.javarosa.core.io.StreamsUtil.OutputIOException;
import org.javarosa.core.log.WrappedException;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.j2me.log.HandledThread;
import org.javarosa.j2me.reference.HttpReference.SecurityFailureListener;
import org.javarosa.j2me.storage.rms.RMSTransaction;
import org.javarosa.model.xform.DataModelSerializer;
import org.javarosa.service.transport.securehttp.AuthenticatedHttpTransportMessage;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportException;
import org.javarosa.services.transport.impl.simplehttp.StreamingHTTPMessage;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;
import org.commcare.core.parse.CommCareTransactionParserFactory;
import org.commcare.util.J2METransactionParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Hashtable;


/**
 * @author ctsims
 *
 */
public class CommCareRestorer implements Runnable {

    protected static final int RESPONSE_NONE = 0;
    protected static final int RESPONSE_YES = 1;
    protected static final int RESPONSE_NO = 2;

    int response = RESPONSE_NONE;

    CommCareOTARestoreTransitions transitions;
    CommCareOTARestoreListener listener;

    String restoreURI;
    boolean noPartial;
    boolean isSync;
    int[] caseTallies;

    HttpAuthenticator authenticator;
    boolean errorsOccurred;
    String syncToken;
    private boolean recoveryMode = false;
    String originalRestoreURI;
    String logSubmitURI;
    String stateHash;

    public void initialize(CommCareOTARestoreListener rListener, CommCareOTARestoreTransitions transitions,
            String restoreURI, HttpAuthenticator authenticator, boolean isSync, boolean noPartial, String syncToken, String logSubmitURI){

        if (isSync && !noPartial) {
            System.err.println("WARNING: no-partial mode is strongly recommended when syncing");
        }
        this.syncToken = syncToken;

        this.originalRestoreURI = restoreURI;
        this.authenticator = authenticator;
        this.logSubmitURI = logSubmitURI;

        this.isSync = isSync;
        if (isSync) {
            this.stateHash = CaseDBUtils.computeHash((IStorageUtility<Case>)StorageManager.getStorage(Case.STORAGE_KEY));
            initURI(syncToken, stateHash);
        } else {
            initURI(null,null);
        }
        this.noPartial = noPartial;

        this.listener = rListener;

        this.transitions = transitions;

        HandledThread t = new HandledThread(this);
        t.start();

    }

    private AuthenticatedHttpTransportMessage getClientMessage(HttpAuthenticator authenticator) {
        AuthenticatedHttpTransportMessage message = AuthenticatedHttpTransportMessage.AuthenticatedHttpRequest(restoreURI, authenticator,
                new SecurityFailureListener() {

            public void onSecurityException(SecurityException e) {
                PeriodicEvent.schedule(new PermissionsEvent());
            }
        });
        return message;
    }

    public void run() {
        if(authenticator != null) {
            AuthenticatedHttpTransportMessage message = getClientMessage(authenticator);
            tryDownload(message);
        } else{
            start();
        }
    }

    public void start() {
        Reference bypassRef = getBypassRef();
        if(bypassRef != null) {
            listener.refreshView();
            tryBypass(bypassRef);
        } else{
            listener.statusUpdate(CommCareOTARestoreListener.REGULAR_START);
            startOtaProcess();
        }
    }

    private void startOtaProcess() {
         if(authenticator == null) {
            getCredentials();
        } else {
            listener.refreshView();

            tryDownload(getClientMessage(authenticator));
        }
    }

    private void getCredentials() {
        listener.getCredentials();
    }

    private void tryDownload(AuthenticatedHttpTransportMessage message) {
        tryDownload(message, true);
    }

    private void tryDownload(AuthenticatedHttpTransportMessage message, boolean tryCache) {
        listener.statusUpdate(CommCareOTARestoreListener.RESTORE_DOWNLOAD);
        Logger.log("restore", "start");
        try {
            if(message.getUrl() == null) {
                //TODO Figure out what's up with this failure
                listener.onFailure(Localization.get("restore.noserveruri"));
                listener.refreshView();
                listener.onFailure(null);
                return;
            }
            AuthenticatedHttpTransportMessage sent = (AuthenticatedHttpTransportMessage)TransportService.sendBlocking(message);
            if(sent.isSuccess()) {
                listener.statusUpdate(CommCareOTARestoreListener.RESTORE_CONNECTION_MADE);
                try {
                    if(tryCache) {
                        downloadRemoteData(sent.getResponse());
                    } else {
                        startRestore(sent.getResponse());
                    }
                    return;
                } catch(IOException e) {
                    listener.statusUpdate(CommCareOTARestoreListener.RESTORE_BAD_DOWNLOAD);
                    listener.promptRetry(Localization.get("restore.fail.transport", new String[] {WrappedException.printException(e)}));
                    return;
                }
            } else {
                if(sent.getResponseCode() == 401) {
                    listener.statusUpdate(CommCareOTARestoreListener.RESTORE_BAD_CREDENTIALS);
                    Logger.log("restore",Localization.get("restore.badcredentials"));
                    getCredentials();
                    return;
                } else if(sent.getResponseCode() == 404) {
                    listener.statusUpdate(CommCareOTARestoreListener.RESTORE_BAD_SERVER);
                    listener.promptRetry(Localization.get("restore.badserver"));
                    return;
                } else if(sent.getResponseCode() == 412) {
                    //Our local copy of the case database has gotten out of sync. We need to start a recovery
                    //process.
                    listener.statusUpdate(CommCareOTARestoreListener.RESTORE_BAD_DB);
                    startRecovery();
                    return;
                } else if(sent.getResponseCode() == 503) {
                    listener.statusUpdate(CommCareOTARestoreListener.RESTORE_DB_BUSY);
                    listener.promptRetry(Localization.get("restore.db.busy"));
                    return;
                } else if(sent.getResponseCode() == 0){
                    listener.promptRetry(Localization.get("restore.fail.nointernet"));
                } else {
                    listener.statusUpdate(CommCareOTARestoreListener.RESTORE_FAIL_OTHER);
                    listener.promptRetry(Localization.get("restore.fail.other", new String[] {sent.getFailureReason()}));
                    return;
                }
            }
        } catch (TransportException e) {
            listener.statusUpdate(CommCareOTARestoreListener.RESTORE_CONNECTION_FAIL_ENTRY);
            listener.promptRetry(Localization.get("restore.fail.transport", new String[] {WrappedException.printException(e)}));
        }
    }

    /**
     * The recovery process comes in three phases. First, reporting to the server all of the cases that
     * currently live on the phone (so the server can compare to its current state).
     *
     * Next, the full restore data is retrieved from the server and stored locally to ensure that the db
     * can be recovered. Then local storage is cleared of data, and
     */
    private void startRecovery() {
        //Make a streaming message (the db is likely be too big to store in memory)
        TransportMessage message = new StreamingHTTPMessage(this.getSubmitUrl()) {
            public void _writeBody(OutputStream os) throws IOException {
                //TODO: This is just the casedb, we actually want
                DataModelSerializer s = new DataModelSerializer(os, new CommCareInstanceInitializer(CommCareStatic.appStringCache));
                s.serialize(new ExternalDataInstance("jr://instance/casedb/report" + "/" + syncToken + "/" + stateHash,"casedb"), null);

            }
        };
        listener.statusUpdate(CommCareOTARestoreListener.RESTORE_RECOVER_SEND);
        try {
            message = TransportService.sendBlocking(message);
        } catch (TransportException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if(message.isSuccess()) {
            //The server is now informed of our current state, time for the tricky part,
            this.recoveryMode  = true;
            initURI(null, null);
            //TODO: Set a flag somewhere (sync token perhaps) that we're in recovery mode
            this.startOtaProcess();
        } else {
            listener.promptRetry("restore.recover.fail");
        }
    }

    public boolean startRestore(InputStream input) {
        listener.refreshView();
        listener.statusUpdate(CommCareOTARestoreListener.RESTORE_START);

        final InputStream fInput = input;

        if(recoveryMode) {
            listener.statusUpdate(CommCareOTARestoreListener.RESTORE_RECOVERY_WIPE);
            //We've downloaded our file and can now recovery state fully for this user, so we need to wipe
            //out existing cases. Ideally we'd do this by renaming the RMS (so we could recover if needed),
            //but for now, just go for it.
            StorageManager.getStorage(Case.STORAGE_KEY).removeAll();
        }

        errorsOccurred = false;

        boolean success = false;
        String[] parseErrors = new String[0];
        String restoreID = null;


        try {
            beginTransaction();
            CommCareTransactionParserFactory factory = new J2METransactionParserFactory(!noPartial);
            DataModelPullParser parser = new DataModelPullParser(fInput,factory,listener);
            parser.requireRootEnvelopeType("OpenRosaResponse");
            success = parser.parse();
            restoreID = factory.getSyncToken();
            caseTallies = factory.getCaseTallies();
            //TODO: Is success here too strict?
            if (success) {
                transitions.commitSyncToken(restoreID);
                PropertyManager._().setProperty(CommCareProperties.LAST_SYNC_AT, DateUtils.formatDateTime(new Date(), DateUtils.FORMAT_ISO8601));
            }
            parseErrors = parser.getParseErrors();

        } catch (IOException e) {
            listener.promptRetry(Localization.get("restore.fail.retry"));
            return false;
        } catch (InvalidStructureException e) {
            listener.promptRetry(Localization.get("restore.fail.technical"));
            Logger.exception(e);
            return false;
        } catch (XmlPullParserException e) {
            listener.promptRetry(Localization.get("restore.fail.technical"));
            Logger.exception(e);
            return false;
        } catch (UnfullfilledRequirementsException e) {
            listener.promptRetry(Localization.get("restore.fail.technical"));
            Logger.exception(e);
            return false;
        } catch (RuntimeException e) {
            Logger.exception(e);
            listener.promptRetry(Localization.get("restore.fail.technical"));
            return false;
        } finally {
            if (success) {
                commitTransaction();
            } else {
                rollbackTransaction();
            }
        }
        if (success) {
            listener.statusUpdate(CommCareOTARestoreListener.RESTORE_SUCCESS);
            Logger.log("restore", "successful: " + (restoreID != null ? restoreID : "???"));
        }
        else {
            if (noPartial) {
                listener.statusUpdate(CommCareOTARestoreListener.RESTORE_FAIL);
            }
            else {
                listener.statusUpdate(CommCareOTARestoreListener.RESTORE_FAIL_PARTIAL);
            }

            Logger.log("restore", (noPartial ? "restore errors; rolled-back" : "unsuccessful or partially successful") +
                    ": " + (restoreID != null ? restoreID : "???"));
            for(String s : parseErrors) {
                Logger.log("restore", "err: " + s);
            }

            errorsOccurred = true;
        }
        listener.onSuccess();
        return success || !noPartial;
    }

    public void tryBypass(final Reference bypass) {
        listener.statusUpdate(CommCareOTARestoreListener.BYPASS_START);

        try {
            Logger.log("restore", "starting bypass restore attempt with file: " + bypass.getLocalURI());
            InputStream restoreStream = bypass.getStream();
            if(startRestore(restoreStream)) {
                try {
                    //Success! Try to wipe the local file and then let the UI handle the rest.
                    restoreStream.close();
                    listener.statusUpdate(CommCareOTARestoreListener.BYPASS_CLEAN);
                    bypass.remove();
                    listener.statusUpdate(CommCareOTARestoreListener.BYPASS_CLEAN_SUCCESS);
                } catch (IOException e) {
                    //Even if we fail to delete the local file, it's mostly fine. Jut let the user know
                    e.printStackTrace();

                    listener.statusUpdate(CommCareOTARestoreListener.BYPASS_CLEANFAIL);
                }
                Logger.log("restore", "bypass restore succeeded");
                return;
            }
        } catch(IOException e) {
            //Couldn't open a stream to the restore file, we'll need to dump out to
            //OTA
            e.printStackTrace();
        }

        //Something bad about the restore file.
        //Skip it and dump back to OTA Restore
        Logger.log("restore", "bypass restore failed, falling back to OTA");
        listener.statusUpdate(CommCareOTARestoreListener.BYPASS_FAIL);
        startOtaProcess();
    }

    /**
     *
     * @param stream
     * @throws IOException If there was a problem which resulted in the cached
     * file being corrupted or unavailable _and_ the input stream becoming invalidated
     * such that a retry is necessary.
     */
    private void downloadRemoteData(InputStream stream) throws IOException {
        listener.refreshView();
        Reference ref;
        try {
            ref = ReferenceManager._().DeriveReference(getCacheRef());

            boolean badCache = false;
            try {
                //Wipe out the file if it exists (and we can)
                if(ref.doesBinaryExist()) {
                    ref.remove();
                }
            } catch(IOException e) {
                badCache = true;
            }

            if(badCache || ref.isReadOnly()) {
                listener.statusUpdate(CommCareOTARestoreListener.RESTORE_NO_CACHE);
                noCache(stream);
            } else {
                OutputStream output;

                //We want to treat any problems dealing with the inability to
                //download as separate from a _failed_ download, which is
                //what this try-catch is all about.
                try {
                    output = ref.getOutputStream();
                }
                catch (Exception e) {
                    if(recoveryMode) {
                        //In recovery mode we can't really afford to not cache this, so report that, and try again.
                        listener.statusUpdate(CommCareOTARestoreListener.RESTORE_NEED_CACHE);
                        return;
                    } else {
                        e.printStackTrace();
                        noCache(stream);
                        return;
                    }
                }

                try {
                    StreamsUtil.writeFromInputToOutputSpecific(stream, output);
                } catch (OutputIOException e) {
                    //So for some reason it's entirely possible to fail on some Nokia phones only while _writing_
                    //to the SD card, not while opening the Stream for writing. This exception should be handled the
                    //same way
                    if(recoveryMode) {
                        //In recovery mode we can't really afford to not cache this, so report that, and try again.
                        listener.statusUpdate(CommCareOTARestoreListener.RESTORE_NEED_CACHE);
                        return;
                    } else {
                        //otherwise we need to start again from the top and not attempt to cache.

                        //first, close out the input stream
                        try { stream.close(); } catch(IOException ioe) { }

                        //Now start over with no local caching
                        this.tryDownload(this.getClientMessage(this.getAuthenticator()), false);
                        return;
                    }
                } catch(InputIOException e) {
                    //Now any further IOExceptions will get handled as "download failed",
                    //rather than "couldn't attempt to download"
                    throw e.getWrapped();
                } finally {
                    try {
                    //need to close file's write stream before we read from it (S60 is not happy otherwise)
                    output.close();
                    } catch(IOException e) {
                        //Stupid Java
                    }
                }

                listener.statusUpdate(CommCareOTARestoreListener.RESTORE_DOWNLOADED);
                startRestore(ref.getStream());
            }
        } catch (InvalidReferenceException e) {
            noCache(stream);
        }

    }

    protected String getCacheRef() {
        return "jr://file/commcare_ota_backup.xml";
    }

    private void initURI (String lastSync, String stateHash) {//TODO add character here

        String baseURI = this.originalRestoreURI;
        if(baseURI.indexOf("verson=2.0") == -1) {
            baseURI = baseURI + (baseURI.indexOf("?") == -1 ? "?" : "&") + "version=2.0";
        }

        //get property
        if (lastSync != null) {
            this.restoreURI = baseURI + (baseURI.indexOf("?") == -1 ? "?" : "&" ) + "since=" + lastSync + "&state=ccsh:" + stateHash;
        } else {
            this.restoreURI = baseURI;
        }

        //add arg to request the count of items in the envelope
        this.restoreURI = restoreURI + (restoreURI.indexOf("?") == -1 ? "?" : "&") + "items=true";

    }

    private void noCache(InputStream input) throws IOException {
        if (this.noPartial) {
            Logger.log("ota-restore", "attempted to restore OTA in 'no partial' mode, but could not cache payload locally");
            throw new IOException();
        } else {
            listener.statusUpdate(CommCareOTARestoreListener.RESTORE_NO_CACHE);
            startRestore(input);
        }
     }

    public Reference getBypassRef() {
        try {
            String bypassRef = PropertyManager._().getSingularProperty(CommCareProperties.OTA_RESTORE_OFFLINE);
            if(bypassRef == null || bypassRef == "") {
                return null;
            }

            Reference bypass = ReferenceManager._().DeriveReference(bypassRef);
            if(bypass == null || !bypass.doesBinaryExist()) {
                return null;
            }
            return bypass;
        } catch(Exception e){
            e.printStackTrace();
            //It would be absurdly stupid if we couldn't OTA restore because of an error here
            return null;
        }

    }

    public Hashtable<String, Integer> getCaseTallies() {
        Hashtable<String, Integer> tall = new Hashtable<String, Integer>();
        tall.put("create", new Integer(this.caseTallies[0]));
        tall.put("update", new Integer(this.caseTallies[1]));
        tall.put("close", new Integer(this.caseTallies[2]));
        return tall;
    }

    private void beginTransaction () {
        if (this.noPartial)
            RMSTransaction.beginTransaction();
    }

    private void commitTransaction () {
        if (this.noPartial)
            RMSTransaction.commitTransaction();
    }

    private void rollbackTransaction () {
        if (this.noPartial)
            RMSTransaction.rollbackTransaction();
    }

    protected void fail(Exception e) {
        listener.onFailure("Fail with: " + e.getMessage());
    }

    private String getSubmitUrl() {
        return logSubmitURI;
    }

    public HttpAuthenticator getAuthenticator(){
        return authenticator;
    }
}
