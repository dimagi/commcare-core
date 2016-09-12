package org.javarosa.xform.util;

import org.javarosa.core.services.locale.Localization;

/**
 * Get localized arrays from the Localization file system (stored as comma separated lists)
 */

public class LocaleArrayDataSource implements ArrayDataSource{
    @Override
    public String[] getArray(String key) {
        return Localization.getArray(key);
    }
}
