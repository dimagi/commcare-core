/**
 *
 */
package org.commcare.cases.attachments;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class CaseAttachmentRecord implements Persistable {
    String url;
    String destination;

    public CaseAttachmentRecord(String url, String destination) {
        this.url = url;
        this.destination = destination;
    }

    /**
     * @return the url
     */
    public String getResourceUrl() {
        return url;
    }

    /**
     * @return the destination
     */
    public String getDestination() {
        return destination;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.Persistable#setID(int)
     */
    public void setID(int ID) {

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.Persistable#getID()
     */
    public int getID() {
        return 0;
    }
}
