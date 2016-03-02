/**
 *
 */
package org.commcare.cases.ledger;

import org.commcare.cases.model.Case;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.DataUtil;

import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * @author ctsims
 */
public class LedgerPurgeFilter extends EntityFilter<Ledger> {
    final Vector<Integer> idsToRemove = new Vector<Integer>();

    /**
     * Create a filter for purging ledgers which should no longer be on the phone from
     * the database. Ledger liveness matches that of the case database, so we can just
     * look for deltas between the two models
     *
     * @param ledgerStorage The storage which is to be cleaned up.
     * @param caseStorage   The case storage database for reference
     */
    public LedgerPurgeFilter(IStorageUtilityIndexed<Ledger> ledgerStorage, IStorageUtilityIndexed<Case> caseStorage) {
        for (IStorageIterator<Ledger> i = ledgerStorage.iterate(); i.hasMore(); ) {
            Ledger s = i.nextRecord();
            try {
                caseStorage.getRecordForValue(Case.INDEX_CASE_ID, s.getEntiyId());
            } catch (NoSuchElementException nsee) {
                idsToRemove.addElement(new Integer(s.getID()));
            }
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.EntityFilter#preFilter(int, java.util.Hashtable)
     */
    public int preFilter(int id, Hashtable<String, Object> metaData) {
        if (idsToRemove.contains(DataUtil.integer(id))) {
            return PREFILTER_INCLUDE;
        } else {
            return PREFILTER_EXCLUDE;
        }
    }

    public boolean matches(Ledger e) {
        //We're doing everything with pre-filtering
        return false;
    }

}
