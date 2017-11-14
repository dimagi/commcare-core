package org.javarosa.core.services.locale;

import org.javarosa.core.reference.ReferenceDataSource;
import org.javarosa.core.util.NoLocalizedTextException;

import java.util.Hashtable;

public class Localization {

    private static final ThreadLocal<Localizer> globalLocalizer = new ThreadLocal<Localizer>(){
        @Override
        protected Localizer initialValue()
        {
            return new Localizer(true, false);
        }
    };

    public static String get(String key) {
        return get(key, new String[]{});
    }

    public static String get(String key, String arg) {
        checkRep();
        return globalLocalizer.get().getText(key, new String[]{arg});
    }

    public static String get(String key, String[] args) {
        checkRep();
        return globalLocalizer.get().getText(key, args);
    }

    public static String get(String key, Hashtable args) {
        checkRep();
        return globalLocalizer.get().getText(key, args);
    }

    public static String getWithDefault(String key, String valueIfKeyMissing) {
        return getWithDefault(key, new String[]{}, valueIfKeyMissing);
    }

    public static String getWithDefault(String key, String[] args, String valueIfKeyMissing) {
        try {
            return get(key, args);
        } catch (NoLocalizedTextException e) {
            return valueIfKeyMissing;
        }
    }

    public static void registerLanguageReference(String localeName, String referenceUri) {
        init(false);
        if (!globalLocalizer.get().hasLocale(localeName)) {
            globalLocalizer.get().addAvailableLocale(localeName);
        }
        globalLocalizer.get().registerLocaleResource(localeName, new ReferenceDataSource(referenceUri));
        if (globalLocalizer.get().getDefaultLocale() == null) {
            globalLocalizer.get().setDefaultLocale(localeName);
        }
    }

    public static Localizer getGlobalLocalizerAdvanced() {
        init(false);
        return globalLocalizer.get();
    }

    public static void setLocale(String locale) {
        checkRep();
        globalLocalizer.get().setLocale(locale);
    }

    public static String getCurrentLocale() {
        checkRep();
        return globalLocalizer.get().getLocale();
    }

    public static void setDefaultLocale(String defaultLocale) {
        checkRep();
        globalLocalizer.get().setDefaultLocale(defaultLocale);
    }

    public static void init(boolean force) {
        if (globalLocalizer.get() == null || force) {
            globalLocalizer.set(new Localizer(true, false));
        }
    }

    private static void checkRep() {
        init(false);
        if (globalLocalizer.get().getAvailableLocales().length == 0) {
            throw new LocaleTextException("There are no locales defined for the application. Please make sure to register locale text using the Locale.register() method");
        }
    }

    public static String[] getArray(String key) {
        return Localization.get(key).split(",");
    }
}
