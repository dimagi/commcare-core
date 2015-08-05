package org.javarosa.core.reference;

import org.javarosa.core.services.locale.LocaleDataSource;
import org.javarosa.core.services.locale.LocalizationUtils;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The ReferenceDataSource is a source of locale data which
 * is located at a location which is defined by a ReferenceURI.
 *
 * @author Clayton Sims
 * @date Jun 1, 2009
 */
public class ReferenceDataSource implements LocaleDataSource {

    String referenceURI;

    /**
     * NOTE: FOR SERIALIZATION ONLY!
     */
    public ReferenceDataSource() {

    }

    /**
     * Creates a new Data Source for Locale data with the given resource URI.
     *
     * @param referenceURI URI to the resource file from which data should be loaded
     * @throws NullPointerException if resourceURI is null
     */
    public ReferenceDataSource(String referenceURI) {
        if (referenceURI == null) {
            throw new NullPointerException("Reference URI cannot be null when creating a Resource File Data Source");
        }
        this.referenceURI = referenceURI;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.locale.LocaleDataSource#getLocalizedText()
     */
    public OrderedHashtable getLocalizedText() {
        InputStream is = null;
        try {
            is = ReferenceManager._().DeriveReference(referenceURI).getStream();
            return LocalizationUtils.parseLocaleInput(is);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IOException while getting localized text at reference " + referenceURI + "\n" + e.getMessage());
        } catch (InvalidReferenceException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid Reference! " + referenceURI);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        referenceURI = in.readUTF();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        out.writeUTF(referenceURI);
    }
}
