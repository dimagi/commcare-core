package org.commcare.util;

import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;

/**
 * Get localized arrays from the Localization file system (stored as comma separated lists)
 */

public class LocaleArrayDataSource implements ArrayDataSource{

    ArrayDataSource fallback = null;

    public LocaleArrayDataSource() {

    }

    public LocaleArrayDataSource(ArrayDataSource fallback) {
        this.fallback = fallback;
    }

    @Override
    public String[] getArray(String key) {
        try {
            return Localization.getArray(key);
        } catch (NoLocalizedTextException e) {
            if (fallback != null) {
                return fallback.getArray(key);
            } else {
                throw e;
            }
        }
    }
}
