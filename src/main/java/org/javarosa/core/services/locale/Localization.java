package org.javarosa.core.services.locale;

import org.javarosa.core.reference.ReferenceDataSource;
import org.javarosa.core.util.NoLocalizedTextException;

import java.util.Hashtable;

public class Localization {

    public static String get(String key) {
        return get(key, new String[]{});
    }

    public static String get(String key, String arg) {
        checkRep();
        return LocalizerManager.getGlobalLocalizer().getText(key, new String[]{arg});
    }

    public static String get(String key, String[] args) {
        checkRep();
        return LocalizerManager.getGlobalLocalizer().getText(key, args);
    }

    public static String get(String key, Hashtable args) {
        checkRep();
        return LocalizerManager.getGlobalLocalizer().getText(key, args);
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
        if (!LocalizerManager.getGlobalLocalizer().hasLocale(localeName)) {
            LocalizerManager.getGlobalLocalizer().addAvailableLocale(localeName);
        }
        LocalizerManager.getGlobalLocalizer().registerLocaleResource(localeName, new ReferenceDataSource(referenceUri));
        if (LocalizerManager.getGlobalLocalizer().getDefaultLocale() == null) {
            LocalizerManager.getGlobalLocalizer().setDefaultLocale(localeName);
        }
    }

    public static Localizer getGlobalLocalizerAdvanced() {
        init(false);
        return LocalizerManager.getGlobalLocalizer();
    }

    public static void setLocale(String locale) {
        checkRep();
        LocalizerManager.getGlobalLocalizer().setLocale(locale);
    }

    public static String getCurrentLocale() {
        checkRep();
        return LocalizerManager.getGlobalLocalizer().getLocale();
    }

    public static void setDefaultLocale(String defaultLocale) {
        checkRep();
        LocalizerManager.getGlobalLocalizer().setDefaultLocale(defaultLocale);
    }

    public static void init(boolean force) {
        LocalizerManager.init(force);
    }

    private static void checkRep() {
        init(false);
        if (LocalizerManager.getGlobalLocalizer().getAvailableLocales().length == 0) {
            throw new LocaleTextException("There are no locales defined for the application. Please make sure to register locale text using the Locale.register() method");
        }
    }

    public static String[] getArray(String key) {
        return Localization.get(key).split(",");
    }
}
