package org.commcare.applogic;

import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareHQResponder;
import org.commcare.util.CommCareUtil;
import org.javarosa.core.api.State;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;
import org.javarosa.service.transport.securehttp.HttpCredentialProvider;
import org.javarosa.services.transport.TransportService;
import org.javarosa.user.model.User;

import java.util.Hashtable;

public abstract class ServerSyncState implements State {
    SendAllUnsentState send;
    CommCareOTARestoreState pull;

    public ServerSyncState () {
        this(CommCareContext._().getCurrentUserCredentials());
    }

    public ServerSyncState (HttpCredentialProvider currentUserCredentials) {
        send = new SendAllUnsentState () {
            protected SendAllUnsentController getController () {
                return new SendAllUnsentController(new CommCareHQResponder(PropertyManager._().getSingularProperty(JavaRosaPropertyRules.OPENROSA_API_LEVEL)), true, true, Localization.get("sync.unsent.cancel"));
            }

            public void done () {
                throw new RuntimeException("method not applicable");
            }

            public void done(boolean errorsOccurred) {
                if (errorsOccurred) {
                    System.out.println("debug: server sync: errors occurred during send-all-unsent");
                    onError(Localization.get("sync.send.fail"));
                } else if(TransportService.getCachedMessagesSize() != 0) {
                    //cancelled
                    onError(Localization.get("sync.cancelled.sending"));
                } else {
                    System.out.println("debug: server sync: send-all-unsent successful");
                    launchPull();
                }
            }
        };


        String syncToken = null;
        User u = CommCareContext._().getUser();
        if(u != null) {
            syncToken = u.getLastSyncToken();
        }

        HttpAuthenticator auth = new HttpAuthenticator(CommCareUtil.wrapCredentialProvider(currentUserCredentials));
        pull = new CommCareOTARestoreState (syncToken, auth, u.getUsername()) {
            public void cancel() {
                //when your credentials have changed, the ota restore credentials screen will pop up, so we
                //do need to support canceling here.
                ServerSyncState.this.onError(Localization.get("sync.cancelled"));
            }

            public void commitSyncToken(String token) {
                if(token != null) {
                    User u = CommCareContext._().getUser();
                    u.setLastSyncToken(token);
                    try {
                        StorageManager.getStorage(User.STORAGE_KEY).write(u);
                    } catch (StorageFullException e) {
                        Logger.die("sync", e);
                    }
                }
            }

            public void done(boolean errorsOccurred) {
                if (errorsOccurred) {
                    System.out.println("debug: server sync: errors occurred during pull-down");
                    onError(Localization.get("sync.pull.fail"));
                } else {
                    onSuccess(restoreDetailMsg(controller.getCaseTallies()));
                }
            }
        };
    }

    public void start() {
        User u = CommCareContext._().getUser();

        //Don't even trigger the sync stuff if we're logged in as a magic user
        //("admin" or the demo user).
        if(CommCareUtil.isMagicAdmin(u)) {
            onSuccess(Localization.get("sync.pull.admin"));
            return;
        }

        else if(User.DEMO_USER.equals(u.getUserType())) {
            onSuccess(Localization.get("sync.pull.demo"));
            return;
        }
        //This involves the global context, and thus should really be occurring in the constructor, but
        //also takes a long time, and thus is more important to not occur until starting.
        CommCareContext._().purgeScheduler(true);

        send.start();
    }

    public void launchPull() {
        J2MEDisplay.startStateWithLoadingScreen(pull);
    }

    public abstract void onError (String detailMsg);
    public abstract void onSuccess (String detailMsg);

    //TODO: customize me
    public String restoreDetailMsg (Hashtable<String, Integer> tallies) {
        int created = tallies.get("create").intValue();
        int updated = tallies.get("update").intValue();
        int closed = tallies.get("close").intValue();

        if (created + updated + closed == 0) {
            return Localization.get("sync.done.noupdate");
        } else {
            String msg = Localization.get("sync.done.updates");
            if (created > 0) {
                msg += (msg.length() > 0 ? "; " : "") + Localization.get("sync.done.new",new String[] {String.valueOf(created)});
            }
            if (closed > 0) {
                msg += (msg.length() > 0 ? "; " : "") + Localization.get("sync.done.closed",new String[] {String.valueOf(closed)});
            }
            if (updated > 0) {
                msg += (msg.length() > 0 ? "; " : "") + Localization.get("sync.done.updated",new String[] {String.valueOf(updated)});
            }
            return msg + ".";
        }
    }

}
