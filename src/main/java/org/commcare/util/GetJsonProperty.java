package org.commcare.util;

import org.json.JSONObject;
import org.json.JSONException;

/** 
 * 
 * @author rcostello
 * @return A String value for the property name passed in if that property exists else a blank String
 */

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
