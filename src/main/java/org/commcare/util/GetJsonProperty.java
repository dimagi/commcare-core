package org.commcare.util;

import org.json.JSONException;
import org.json.JSONObject;

public class GetJsonProperty {

    public static String getJsonProperty(String stringifiedJsonOblect, String propertyName) {
        JSONObject parsedObject = new JSONObject(stringifiedJsonOblect);
        String value = "";
        try {
            value = parsedObject.getString(propertyName);
        } catch (JSONException e) {
            return value;
        }

        return value;
    }
}
