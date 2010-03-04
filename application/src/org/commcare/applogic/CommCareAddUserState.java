package org.commcare.applogic;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.util.CommCareUserDecorator;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.user.api.AddUserController;
import org.javarosa.user.api.AddUserState;
import org.javarosa.user.api.RegisterUserState;
import org.javarosa.user.model.User;

public class CommCareAddUserState extends AddUserState {

	protected AddUserController getController () {
		return new AddUserController(AddUserController.PASSWORD_FORMAT_NUMERIC, new CommCareUserDecorator());
	}
	
	public void cancel() {
		new CommCareHomeState().start();
	}

	public void userAdded(User newUser) {
		//#if commcare.user.registration
		
		new RegisterUserState(newUser) {
			
			public String getRegistrationURL() {
				return PropertyManager._().getSingularProperty(CommCareProperties.POST_URL_PROPERTY);
			}

			public void cancel() {
				new CommCareHomeState().start();
			}

			public void succesfullyRegistered(User user) {
				IStorageUtility users = StorageManager.getStorage(User.STORAGE_KEY);
				try {
					users.write(user);
				} catch (StorageFullException e) {
					throw new RuntimeException("uh-oh, storage full [users]"); //TODO: handle this
				}
				
				new CommCareHomeState().start();
			}
			
		}.start();
		
		//#else
		
		new CommCareHomeState().start();
		
		//#endif
	}
	
}
