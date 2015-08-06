/**
 *
 */
package org.javarosa.j2me.storage.rms;

import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.j2me.storage.rms.raw.RMS;

import java.util.Hashtable;

import javax.microedition.rms.RecordStoreNotOpenException;

/**
 * @author ctsims
 *
 */
public class IdIndex {
    private Hashtable cache;
    private RMS indexStore;
    private Hashtable commitData;

    public IdIndex(RMS indexStore) {
        this.indexStore = indexStore;
    }

    protected void beginChangeCommit(int recordId, RMSRecordLoc newLoc) {
        //Only one commit at a time. We should really block here somehow?
        if(commitData == null) {
            commitData = getIndexTable();
            commitData.put(new Integer(recordId), newLoc);
        } else {
            throw new RuntimeException("Implement Index Commit Blocking!");
        }
    }

    protected void beginChangeCommit(Hashtable commitTable) {
        if(commitData == null) {
            commitData = commitTable;
        } else {
            throw new RuntimeException("Implement Index Commit Blocking!");
        }

    }

    protected boolean commitChange() {
        if(commitData == null) {
            throw new RuntimeException("Implement index commit safety");
        } else {
            boolean outcome = writeIndexTable(commitData);
            commitData = null;
            return outcome;
        }
    }

    protected boolean containsRecordLoc(int recordId) {
        return getIndexTable().containsKey(new Integer(recordId));
    }

    protected RMSRecordLoc getRecordLoc(int recordId) {
        Hashtable idIndex = getIndexTable();
        Integer recordID = new Integer(recordId);
        if (idIndex.containsKey(recordID)) {
            return (RMSRecordLoc)idIndex.get(recordID);
        }
        return null;

    }

    protected Hashtable getIndexTable() {
        if(cache != null) {
            return cache;
        } else {
            Hashtable index = (Hashtable)getIndexStore ().readRecord(RMSStorageUtility.ID_INDEX_REC_ID, new ExtWrapMap(Integer.class, RMSRecordLoc.class));
            if(index == null) {
                throw new RuntimeException("corrupted index table!");
            }
            return index;
        }
    }

    private boolean writeIndexTable(Hashtable idIndex) {
        return getIndexStore ().updateRecord(RMSStorageUtility.ID_INDEX_REC_ID, ExtUtil.serialize(new ExtWrapMap(idIndex)), true);
    }

    /**
     * Return a reference to the indexing/meta-data RMS. Should always access it through this function, as it will re-open
     * the RMS if it was closed by another thread.
     *
     * @return reference to index/meta-data RMS
     */
    protected RMS getIndexStore () {
        indexStore.ensureOpen();
        return indexStore;
    }

    protected int getIndexStoreSize() throws RecordStoreNotOpenException {
        return getIndexStore().rms.getSize();
    }

    protected void release() {
        indexStore.close();
    }

    protected void enterReadOnly() {
        cache = getIndexTable();
    }

    protected void exitReadOnly() {
        cache = null;
    }
}
