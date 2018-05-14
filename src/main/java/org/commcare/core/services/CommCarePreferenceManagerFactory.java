package org.commcare.core.services;

import javax.annotation.Nullable;

/**
 * Wiring to allow access to Android preferences in commcare-core, but can potentially be used for any temporary key value storage
 */
public class CommCarePreferenceManagerFactory {

    private static ICommCarePreferenceManager sCommCarePreferenceManager;

    public static void init(ICommCarePreferenceManager mCommCarePreferenceManager) {
        sCommCarePreferenceManager = mCommCarePreferenceManager;
    }

    @Nullable
    public static ICommCarePreferenceManager getCommCarePreferenceManager() {
        return sCommCarePreferenceManager;
    }
}
