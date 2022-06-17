package org.commcare.util;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * <p>Tests for the GetJsonProperty
 *
 * @author rcostello
 */

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
