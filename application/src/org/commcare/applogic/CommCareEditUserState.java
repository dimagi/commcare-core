package org.commcare.applogic;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.util.CommCareContext;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.user.api.EditUserFormEntryState;
import org.javarosa.user.api.RegisterUserState;
import org.javarosa.user.model.User;

public class CommCareEditUserState extends EditUserFormEntryState {
	
	public CommCareEditUserState(User u) {
		super(u, PropertyManager._().getSingularProperty(CommCareProperties.USER_REG_NAMESPACE),CommCareContext._().getPreloaders(), CommCareContext._().getFuncHandlers());
	}

	public void cancel() {
		new CommCareHomeState().start();
	}

	public void userEdited(User newUser) {
		//#if commcare.user.registration
		
		new RegisterUserState(newUser) {
			
			public String getRegistrationURL() {
				return CommCareContext._().getSubmitURL();
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

	public void abort() {
		cancel();
	}
	
}
