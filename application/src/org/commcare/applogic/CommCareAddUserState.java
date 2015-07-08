package org.commcare.applogic;

import java.util.Vector;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.util.CommCareContext;
import org.commcare.util.OpenRosaApiResponseProcessor;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;
import org.javarosa.user.api.CreateUserFormEntryState;
import org.javarosa.user.api.RegisterUserController;
import org.javarosa.user.api.RegisterUserState;
import org.javarosa.user.model.User;
import org.javarosa.user.transport.HttpUserRegistrationTranslator;

public class CommCareAddUserState extends CreateUserFormEntryState {

    boolean requireRegistration;
    String orApiVersion;

    public CommCareAddUserState(boolean requireRegistration, String orApiVersion) {
        super(PropertyManager._().getSingularProperty(CommCareProperties.USER_REG_NAMESPACE),filterPreloaders(CommCareContext._().getPreloaders()), CommCareContext._().getFuncHandlers());
        this.requireRegistration = requireRegistration;
        this.orApiVersion = orApiVersion;
    }

    public static Vector<IPreloadHandler> filterPreloaders(Vector<IPreloadHandler> preloaders) {
        Vector<IPreloadHandler> toRemove = new Vector<IPreloadHandler>();
        for(IPreloadHandler preloader : preloaders) {
            if(preloader.preloadHandled().equals("user")) {
                toRemove.addElement(preloader);
            }
        }
        for(IPreloadHandler pending : toRemove) {
            preloaders.removeElement(pending);
        }
        return preloaders;
    }

    public void cancel() {
        J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
    }

    public void userCreated(final User newUser) {
        IStorageUtility userStorage = StorageManager.getStorage(User.STORAGE_KEY);
        //We could get this by meta, but it's hard to track usernames due to case issues
        for(IStorageIterator iterator = userStorage.iterate(); iterator.hasMore();) {
            User user = (User)iterator.nextRecord();
            if(user.getUsername().toLowerCase().equals(newUser.getUsername().toLowerCase()) &&
                    !user.getUniqueId().equals(newUser.getUniqueId())) {
                //Duplicate username. Don't complete registration!
                //TODO: Can we be confident that just not-going-anywhere here will
                //be the right thing to do? It should just keep us on the end question
                J2MEDisplay.showError(null,Localization.get("activity.adduser.problem.nametaken", new String[] {newUser.getUsername()}));
                return;
            }
        }




        if(requireRegistration) {

            J2MEDisplay.startStateWithLoadingScreen(new RegisterUserState(newUser, orApiVersion) {

                public String getRegistrationURL() {
                    return CommCareContext._().getSubmitURL();
                }

                public void cancel() {
                    J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
                }

                protected RegisterUserController<SimpleHttpTransportMessage> getController () {
                    return new RegisterUserController<SimpleHttpTransportMessage>(new HttpUserRegistrationTranslator(user,getRegistrationURL(), orApiVersion), !OpenRosaApiResponseProcessor.ONE_OH.equals(orApiVersion));
                }

                public void succesfullyRegistered(User user) {
                    IStorageUtility users = StorageManager.getStorage(User.STORAGE_KEY);
                    try {
                        //If the incoming user is null, there were no updates
                        users.write(user == null ? newUser : user);
                    } catch (StorageFullException e) {
                        throw new RuntimeException("uh-oh, storage full [users]"); //TODO: handle this
                    }

                    J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
                }

            });
        } else {
            try {
                userStorage.write(newUser);
            } catch (StorageFullException e) {
                throw new RuntimeException("uh-oh, storage full [users]"); //TODO: handle this
            }
            J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
        }
    }

    public void abort() {
        cancel();
    }

}
