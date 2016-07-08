package org.javarosa.core.services.locale;

import org.javarosa.core.util.externalizable.Externalizable;

import java.util.Hashtable;

/**
 * @author Clayton Sims
 */
public interface LocaleDataSource extends Externalizable {
    Hashtable<String, String> getLocalizedText();
}
