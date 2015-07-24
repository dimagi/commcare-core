package org.commcare.cases;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.javarosa.core.api.IModule;
import org.javarosa.core.services.storage.StorageManager;

/**
 * @author Clayton Sims
 * @date Mar 19, 2009
 */
public class CaseManagementModule implements IModule {

    /* (non-Javadoc)
     * @see org.javarosa.core.api.IModule#registerModule(org.javarosa.core.Context)
     */
    public void registerModule() {
        StorageManager.registerStorage(Case.STORAGE_KEY, Case.class);
        StorageManager.registerStorage(Ledger.STORAGE_KEY, Ledger.class);
    }
}
