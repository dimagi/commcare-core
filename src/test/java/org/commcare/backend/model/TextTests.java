package org.commcare.backend.model;

import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Hashtable;

/**
 * Created by ctsims on 12/14/2016.
 */

public class TextTests {
    @Test
    public void testXPathExceptionHandling() throws Exception {
        boolean thrown = false;
        Hashtable<String, Text> arguments = new Hashtable<>();
        Text t = Text.XPathText("date('steve')", arguments);
        try {
            t.evaluate(new EvaluationContext(null));
        } catch(Exception e) {
            if(!(e instanceof XPathException)) {
                Assert.fail("Invalid exception thrown during XPath text usage");
            }
            e.getMessage();
            thrown = true;
        }
        if(!thrown) {
            Assert.fail("XPath failure in text run did not fail fast");
        }
    }

}
