package org.commcare.util;

import org.json.JSONArray;

import javax.annotation.Nullable;

public class JsonUtils {

    /**
     * Converts a JSON Array to a String Array
     * @param jsonArray A JSON Array containing string objects that we wish to convert to a String Array
     * @return A String Array representation for the given jsonArray
     */
    public static String[] toArray(@Nullable JSONArray jsonArray) {
        if (jsonArray != null) {
            String[] stringArray = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                stringArray[i] = jsonArray.optString(i);
            }
            return stringArray;
        }
        return new String[0];
    }
}
