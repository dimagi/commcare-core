package org.commcare.xml.bulk;

import org.commcare.cases.model.Case;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.kxml2.io.KXmlParser;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;

/**
 * Implementation of BulkProcessingCaseXML Parser which can be used without needing platform
 * specific indexing / db code, but which essentially has no performance gains over a linear
 * parser, and probably operates slower than one.
 *
 * Created by ctsims on 3/14/2017.
 */

public class LinearBulkProcessingCaseXmlParser extends BulkProcessingCaseXmlParser  {
    private IStorageUtilityIndexed storage;

    public LinearBulkProcessingCaseXmlParser(KXmlParser parser, IStorageUtilityIndexed storage) {
        super(parser);
        this.storage = storage;
    }

    @Override
    protected void performBulkRead(Set<String> currentBulkReadSet, Map<String, Case> currentOperatingSet) {
        for(String index : currentBulkReadSet) {
            Case c = retrieve(index);
            if(c != null) {
                currentOperatingSet.put(index, c);
            }
        }
    }

    @Override
    protected void performBulkWrite(SortedMap<String, Case> writeLog) throws IOException {
        for(Case c : writeLog.values()) {
            commit(c);
        }
    }

    private void commit(Case parsed) throws IOException {
        storage.write(parsed);
    }

    protected Case retrieve(String entityId) {
        try {
            return (Case)storage.getRecordForValue(Case.INDEX_CASE_ID, entityId);
        } catch (NoSuchElementException nsee) {
            return null;
        }
    }



}
