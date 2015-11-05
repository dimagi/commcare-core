package org.commcare.applogic;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.util.CommCareContext;
import org.commcare.util.OpenRosaApiResponseProcessor;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;
import org.javarosa.user.api.EditUserFormEntryState;
import org.javarosa.user.api.RegisterUserController;
import org.javarosa.user.api.RegisterUserState;
import org.javarosa.core.model.User;
import org.javarosa.user.transport.HttpUserRegistrationTranslator;

public class CommCareEditUserState extends EditUserFormEntryState {
    boolean registrationRequired;

    String orApiVersion;

    public CommCareEditUserState(User u, boolean registrationRequired, String orApiVersion) {
        super(u, PropertyManager._().getSingularProperty(CommCareProperties.USER_REG_NAMESPACE),CommCareAddUserState.filterPreloaders(CommCareContext._().getPreloaders()), CommCareContext._().getFuncHandlers());
        this.registrationRequired = registrationRequired;
        this.orApiVersion = orApiVersion;
    }

    public void cancel() {
        J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
    }

    public void userEdited(final User newUser) {
        if(registrationRequired) {
            new RegisterUserState(newUser, orApiVersion) {

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
                    //If the incoming user is null, there were no updates
                    users.write(user == null ? newUser : user);

                    J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
                }

            }.start();
        } else {
            IStorageUtility users = StorageManager.getStorage(User.STORAGE_KEY);
            users.write(newUser);

            J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
        }
    }

    public void abort() {
        cancel();
    }

}
