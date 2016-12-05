package org.javarosa.core.model.data.test;

import org.javarosa.core.model.data.GeoPointData;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class GeoPointDataTests {

    @Test
    public void testGetData() {
        double[] pointsA = {1.11111, 2.2, -1.111, -4.19999};
        double[] pointsB = {1, 2, -3, 4};
        double[] pointsC = {6.899999999, 3.20000001};
        double[] pointsD = {6, 3, 0.0000000000000001, 0.00000009};
        GeoPointData data = new GeoPointData(pointsA);
        assertTrue("GeoPointData test constructor and decimal truncation",
                "1.11111 2.2 -1.12 -4.2".equals(data.getDisplayText()));
        data.setValue(pointsB);
        assertTrue("GeoPointData test setValue on 4 datapoints and decimal truncation",
                "1.0 2.0 -3.0 4.0".equals(data.getDisplayText()));
        data.setValue(pointsC);
        assertTrue("GeoPointData test setValue on 2 datapoints",
                "6.899999999 3.20000001".equals(data.getDisplayText()));
        data.setValue(pointsD);
        assertTrue("GeoPointData test setValue on 4 datapoints and decimal truncation",
                "6.0 3.0 0.0 0.0".equals(data.getDisplayText()));
    }
}
