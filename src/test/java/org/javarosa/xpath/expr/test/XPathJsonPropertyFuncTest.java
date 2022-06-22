package org.javarosa.xpath.expr.test;

import org.javarosa.xpath.expr.XPathJsonPropertyFunc;
import org.junit.Test;
import org.junit.Assert;
import org.json.JSONException;

/**Tests for the XPathJsonProperty
 *
 * @author rcostello
 */

import static org.junit.Assert.assertTrue;

public class XPathJsonPropertyFuncTest {

    @Test
    public void getJsonProperty() {
        String testObj1 = "{\"name\":\"Sam\"}";
        String testVal1 = XPathJsonPropertyFunc.getJsonProperty(testObj1, "name");
        String testVal2 = XPathJsonPropertyFunc.getJsonProperty(testObj1, "city");
        Assert.assertEquals(testVal1, "Sam");
        Assert.assertEquals(testVal2, "");

        String testObj2 = "{city: New York}";
        String testVal3 = XPathJsonPropertyFunc.getJsonProperty(testObj2, "city");
        String testVal4 = XPathJsonPropertyFunc.getJsonProperty(testObj2, "state");
        Assert.assertEquals(testVal3, "New York");
        Assert.assertEquals(testVal4, "");

        String testInvalidObj = "{\"name\"}: \"Sam\"}";
        String testVal5 = XPathJsonPropertyFunc.getJsonProperty(testInvalidObj, "name");  
        Assert.assertEquals(testVal5, "");
        
        String testEmptyStrObj = "";
        String testVal6 = XPathJsonPropertyFunc.getJsonProperty(testEmptyStrObj, "name");  
        Assert.assertEquals(testVal6, "");
    }
}
