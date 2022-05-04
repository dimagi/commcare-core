package org.commcare.util;


import org.json.JSONArray;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JsonUtilsTest {

    @Test
    public void toArray() {
        JSONArray jsonArray = new JSONArray();
        int count = 3;
        for (int i = 0; i < count; i++) {
            jsonArray.put("item_" + i);
        }
        String[] stringArray = JsonUtils.toArray(jsonArray);
        assertTrue(stringArray.length == 3);
        assertTrue(stringArray[0].contentEquals("item_0"));
        assertTrue(stringArray[1].contentEquals("item_1"));
        assertTrue(stringArray[2].contentEquals("item_2"));
    }
}
