package org.commcare.test.utilities;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.parse.ParseUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

/**
 * Holds potential configuration profile hooks that let tests run over lots of different scopes
 * a bit more easily
 *
 * Created by ctsims on 3/15/2017.
 */

public class TestProfileConfiguration {
    private boolean useBulkCaseProcessing = false;

    public static Collection BulkOffOn() {
        Object[][] data = new Object[][] { { new TestProfileConfiguration(true) }, { new TestProfileConfiguration(false) }};
        return Arrays.asList(data);
    }

    public TestProfileConfiguration(boolean useBulkCaseProcessing) {
        this.useBulkCaseProcessing = useBulkCaseProcessing;
    }

    @Override
    public String toString() {
        return String.format("Bulk Parse[%s]", useBulkCaseProcessing ? "On" : "Off");
    }

    public void parseIntoSandbox(InputStream stream, UserSandbox sandbox)
            throws InvalidStructureException, UnfullfilledRequirementsException, XmlPullParserException, IOException {
        ParseUtils.parseIntoSandbox(stream, sandbox, false, useBulkCaseProcessing);
    }


    public void parseIntoSandbox(InputStream stream, UserSandbox sandbox, boolean failfast)
            throws InvalidStructureException, UnfullfilledRequirementsException, XmlPullParserException, IOException {
        ParseUtils.parseIntoSandbox(stream, sandbox, failfast, useBulkCaseProcessing);
    }
}
