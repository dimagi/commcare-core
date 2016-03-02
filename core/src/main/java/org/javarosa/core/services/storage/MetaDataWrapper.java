/**
 *
 */
package org.javarosa.core.services.storage;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * An internal-use class to keep track of metadata records without requiring
 * the original object to remain in memory
 *
 * @author ctsims
 */
public class MetaDataWrapper implements IMetaData {
    private final Hashtable<String, Object> data;

    public MetaDataWrapper(Hashtable<String, Object> data) {
        this.data = data;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IMetaData#getMetaDataFields()
     */
    public String[] getMetaDataFields() {
        String[] fields = new String[data.size()];
        int count = 0;
        for (Enumeration en = data.keys(); en.hasMoreElements(); ) {
            String field = (String)en.nextElement();
            fields[count] = field;
        }
        return fields;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IMetaData#getMetaData(java.lang.String)
     */
    public Object getMetaData(String fieldName) {
        return data.get(fieldName);
    }

}
