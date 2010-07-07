package org.commcare.applogic;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.util.CommCareContext;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.user.api.AddUserFormEntryState;
import org.javarosa.user.api.RegisterUserState;
import org.javarosa.user.model.User;

public class CommCareAddUserState extends AddUserFormEntryState {
	
	public CommCareAddUserState() {
		super(PropertyManager._().getSingularProperty(CommCareProperties.USER_REG_NAMESPACE),CommCareContext._().getPreloaders(), CommCareContext._().getFuncHandlers());
	}

	public void cancel() {
		J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
	}

	public void userAdded(User newUser) {
		//#if commcare.user.registration
		
		new RegisterUserState(newUser) {
			
			public String getRegistrationURL() {
				return CommCareContext._().getSubmitURL();
			}

			public void cancel() {
				J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
			}

			public void succesfullyRegistered(User user) {
				IStorageUtility users = StorageManager.getStorage(User.STORAGE_KEY);
				try {
					users.write(user);
				} catch (StorageFullException e) {
					throw new RuntimeException("uh-oh, storage full [users]"); //TODO: handle this
				}
				
				J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
			}
			
		}.start();
		
		//#else
		
		J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
		
		//#endif
	}

	public void abort() {
		cancel();
	}
	
}
