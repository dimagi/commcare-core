package org.commcare.cases.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Created by willpride on 10/27/16.
 */

public class StringUtils {

    private static Pattern diacritics;

    //TODO: Really not sure about this size. Also, the LRU probably isn't really the best model here
    //since we'd _like_ for these caches to get cleaned up at _some_ point.
    static final private int cacheSize = 100 * 1024;
    //TODO: Bro you can't just cache every fucking string ever.
    private static LruCache<String, String> normalizationCache;

    /**
     * @param input A non-null string
     * @return a canonical version of the passed in string that is lower cased and has removed diacritical marks
     * like accents.
     */
    public synchronized static String normalize(String input) {
        if (normalizationCache == null) {
            normalizationCache = new LruCache<>(cacheSize);

            diacritics = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        }
        String cachedString = normalizationCache.get(input);
        if (cachedString != null) {
            return cachedString;
        }

        //Initialized the normalized string (If we can, we'll use the Normalizer API on it)
        String normalized = input;

        // TODO: commented out this version check. What's up with that
        //If we're above gingerbread we'll normalize this in NFD form
        //which helps a lot. Otherwise we won't be able to clear up some of those
        //issues, but we can at least still eliminate diacritics.
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
        normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        //} else {
            //TODO: I doubt it's worth it, but in theory we could run
            //some other normalization for the minority of pre-API9
            //devices.
        //}

        String output = diacritics.matcher(normalized).replaceAll("").toLowerCase();

        normalizationCache.put(input, output);

        return output;
    }
}
