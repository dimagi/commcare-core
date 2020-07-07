package org.javarosa.xform.parse;

import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.test.FormParseInit;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ctsims on 07/07/2020
 */

public class TextEncodingTests {
    @Test
    public void testUnicode() {
        FormParseInit fpi = new FormParseInit("/xform_tests/itext_encoding.xml");
        Localizer l = fpi.getFormDef().getLocalizer();
        Assert.assertEquals("\uD83E\uDDD2", l.getText("four_byte_emoji","en"));
    }

}
