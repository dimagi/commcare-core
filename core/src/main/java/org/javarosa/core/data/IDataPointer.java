package org.javarosa.core.data;

import org.javarosa.core.util.externalizable.Externalizable;

import java.io.IOException;
import java.io.InputStream;

/**
 * A data pointer representing a pointer to a (usually) larger object in memory.
 *
 * @author Cory Zue
 */
public interface IDataPointer extends Externalizable {

    /**
     * Get a display string that represents this data.
     */

    String getDisplayText();

    /**
     * Get the data from the underlying storage.  This should maybe be a stream instead of a byte[]
     */
    byte[] getData() throws IOException;

    /**
     * Get the data from the underlying storage.
     */
    InputStream getDataStream() throws IOException;

    /**
     * Deletes the underlying data from storage.
     */
    boolean deleteData();

    /**
     * @return Gets the length of the data payload
     */
    long getLength();
}
