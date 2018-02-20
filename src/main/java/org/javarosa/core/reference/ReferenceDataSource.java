package org.javarosa.core.reference;

import org.javarosa.core.services.locale.LocaleDataSource;
import org.javarosa.core.services.locale.LocalizationUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * The ReferenceDataSource is a source of locale data which
 * is located at a location which is defined by a ReferenceURI.
 *
 * @author Clayton Sims
 */
public class ReferenceDataSource implements LocaleDataSource {

    private String referenceURI;

    @SuppressWarnings("unused")
    public ReferenceDataSource() {
        // for serialization
    }

    /**
     * Creates a new Data Source for Locale data with the given resource URI.
     *
     * @param referenceURI URI to the resource file from which data should be loaded
     */
    public ReferenceDataSource(String referenceURI) {
        if (referenceURI == null) {
            throw new NullPointerException("Reference URI cannot be null when creating a Resource File Data Source");
        }
        this.referenceURI = referenceURI;
    }

    @Override
    public Hashtable<String, String> getLocalizedText() {
        InputStream is = null;
        try {
            is = ReferenceManagerHandler.instance().DeriveReference(referenceURI).getStream();
            if (is == null) {
                throw new IOException("There is no resource at " + referenceURI);
            }
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

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        referenceURI = in.readUTF();
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        out.writeUTF(referenceURI);
    }
}
