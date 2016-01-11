package org.javarosa.core.model.test;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.test_utils.ExprEvalUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Before;
import org.junit.Test;

public class GeoPointTests {

    private FormParseInit geopointFpi;
    private EvaluationContext geopointEvalCtx;

    @Before
    public void setUp() {
        geopointFpi = new FormParseInit("/geopoint_tests.xml");
        geopointEvalCtx = geopointFpi.getFormDef().getEvaluationContext();
    }

    /**
     * Tests basic functionality of the `distance` xpath function that
     * determines how far apart two geopoints are from each other.
     */
    @Test
    public void testDistanceFunctionBetweenSamePoint() throws XPathSyntaxException {
        ExprEvalUtils.assertEqualsXpathEval("Make sure the distance from a point to itself is 0",
                0.0, "distance(/data/random_geopoint, /data/random_geopoint)", geopointEvalCtx);
        ExprEvalUtils.assertEqualsXpathEval("Make sure the distance from a point to itself is 0",
                0.0, "distance(/data/geo_with_acc_and_alt, /data/geo_with_acc_and_alt)",
                geopointEvalCtx);
    }

    /**
     * Tests distance from New York to San Francisco with a tolerance of 1m.
     * Note that New York and San Francisco are given as strings.
     */
    @Test
    public void testDistanceFunctionBetweenDifferentPoints() throws XPathSyntaxException {
        ExprEvalUtils.assertAlmostEqualsXpathEval(
                4127316.0, 1.0, "distance(/data/new_york, /data/san_francisco)", geopointEvalCtx);
    }
}
