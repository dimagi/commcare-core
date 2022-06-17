package org.commcare.util;

import org.json.JSONException;
import org.json.JSONObject;

/** Utility for hidden values as geocoder receivers
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
