/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.core.model.data.test;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

import org.javarosa.core.model.data.GeoPointData;

public class GeoPointDataTests extends TestCase {

    private static int NUM_TESTS = 1;

    public GeoPointDataTests(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
    }

    public GeoPointDataTests(String name) {
        super(name);
    }

    public GeoPointDataTests() {
        super();
    }

    public Test suite() {
        TestSuite aSuite = new TestSuite();

        for (int i = 1; i <= NUM_TESTS; i++) {
            final int testID = i;

            aSuite.addTest(new GeoPointDataTests("GeoPointData Test " + i, new TestMethod() {
                public void run(TestCase tc) {
                    ((GeoPointDataTests)tc).testMaster(testID);
                }
            }));
        }

        return aSuite;
    }

    public void testMaster(int testID) {
        switch (testID) {
            case 1:
                testGetData();
                break;
            default:
                break;
        }
    }

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
