package org.javarosa.core.model.test;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Test;

public class GeoPointTests {
    /**
     * Tests basic functionality of the `distance` xpath function that
     * determines how far apart two geopoints are from each other
     */
    @Test
    public void testDistanceFunction() throws XPathSyntaxException {
        FormParseInit fpi = new FormParseInit("/geopoint_tests.xml");

        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        // step through entire form
        do {
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);

        EvaluationContext evalCtx = fpi.getFormDef().getEvaluationContext();
        /*
        // Uncomment this when 'distance' is implemented to test it works
        ExprEvalUtils.assertEqualsXpathEval("Make sure the distance from a point to itself is 0",
                "0.0", "distance(/data/custom_location, /data/custom_location)", evalCtx);
        */

        // extend with more tests
    }
}
