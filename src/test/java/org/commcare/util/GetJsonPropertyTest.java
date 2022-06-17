package org.commcare.util;

import org.junit.Test;
import org.junit.Assert;

import static org.junit.Assert.assertTrue;

public class GetJsonPropertyTest {

    @Test
    public void getJsonProperty() {
        String testObj1 = "{name: Sam}";
        String testVal1 = GetJsonProperty.getJsonProperty(testObj1, "name");
        String testVal2 = GetJsonProperty.getJsonProperty(testObj1, "city");
        Assert.assertEquals(testVal1, "Sam");
        Assert.assertEquals(testVal2, "");
    }
}
