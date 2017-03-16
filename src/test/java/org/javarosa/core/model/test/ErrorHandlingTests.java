package org.javarosa.core.model.test;

import org.javarosa.core.test.FormParseInit;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Test;

/**
 * Tests for how errors are handling while parsing and executing XForms
 *
 * Created by ctsims on 2/20/2017.
 */

public class ErrorHandlingTests {
    /**
     * Tests that XPath errors are handled as _parse_ errors during runtime, not as runtime errors
     */
    @Test(expected = XFormParseException.class)
    public void testProperErrorHandlingForFormArgs() {
        FormParseInit fpi = new FormParseInit("/xform_tests/xpath_args_parse_error.xml");
    }


}
