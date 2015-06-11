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

package org.javarosa.core.util.test;

import org.javarosa.core.services.locale.LocalizationUtils;
import org.javarosa.core.util.OrderedHashtable;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class LocalizationTests extends TestCase {

    private static int NUM_TESTS = 2;

    /* (non-Javadoc)
     * @see j2meunit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    public LocalizationTests(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
    }

    public LocalizationTests(String name) {
        super(name);
    }

    public LocalizationTests() {
        super();
    }

    public Test suite() {
        TestSuite aSuite = new TestSuite();

        for (int i = 1; i <= NUM_TESTS; i++) {
            final int testID = i;

            aSuite.addTest(new LocalizationTests("Localization Test " + i, new TestMethod() {
                public void run(TestCase tc) {
                    ((LocalizationTests)tc).testMaster(testID);
                }
            }));
        }

        return aSuite;
    }

    public void testMaster(int testID) {
        //System.out.println("running " + testID);

        switch (testID) {
            case 1:
                testParseValue();
                break;
            case 2:
                testParseAndAdd();
                break;
        }
    }

    public void testParseValue() {

        String result1 = "";
        String result2 = "";

        result1 = LocalizationUtils.parseValue("1. One \\n2. Two \\n3. Three");
        result2 = LocalizationUtils.parseValue("\\# Start");

        assertTrue("Parsed newlines correctly", result1.equals("1. One \n2. Two \n3. Three"));
        assertTrue("Parsed newlines correctly", result2.equals("# Start"));
    }

    public void testParseAndAdd() {

        OrderedHashtable<String, String> testTable = new OrderedHashtable<String, String>();

        LocalizationUtils.parseAndAdd(testTable, "string.1=this line should be cutoff here# this is bad if present", 0);
        LocalizationUtils.parseAndAdd(testTable, "string.2=this line should be cutoff here after the space # this is bad is present", 0);
        LocalizationUtils.parseAndAdd(testTable, "string.3=this line should all be here#", 0);
        LocalizationUtils.parseAndAdd(testTable, "string.4=this line should all be here #", 0);
        LocalizationUtils.parseAndAdd(testTable, "string.5=this line should be all here \\# including this \\# and this", 0);
        LocalizationUtils.parseAndAdd(testTable, "string.6=this line should be here \\# and this# not this", 0);
        LocalizationUtils.parseAndAdd(testTable, "string.7=this line should be here \\# and this# not this \\# not this either", 0);
        LocalizationUtils.parseAndAdd(testTable, "string.8=we have a hash \\# and a newline \\n", 0);
        LocalizationUtils.parseAndAdd(testTable, "string.9=this line should all be here with the hash \\#", 0);
        // make sure doesn't crash
        LocalizationUtils.parseAndAdd(testTable, "# this is the whole line", 0);

        assertTrue("Line 1 failed: " + testTable.get("string.1"), testTable.get("string.1").equals("this line should be cutoff here"));
        assertTrue("Line 2 failed: " + testTable.get("string.2"), testTable.get("string.2").equals("this line should be cutoff here after the space "));
        assertTrue("Line 3 failed: " + testTable.get("string.3"), testTable.get("string.3").equals("this line should all be here"));
        assertTrue("Line 4 failed: " + testTable.get("string.4"), testTable.get("string.4").equals("this line should all be here "));
        assertTrue("Line 5 failed: " + testTable.get("string.5"), testTable.get("string.5").equals("this line should be all here # including this # and this"));
        assertTrue("Line 6 failed: " + testTable.get("string.6"), testTable.get("string.6").equals("this line should be here # and this"));
        assertTrue("Line 7 failed: " + testTable.get("string.7"), testTable.get("string.7").equals("this line should be here # and this"));
        assertTrue("Line 8 failed: " + testTable.get("string.8"), testTable.get("string.8").equals("we have a hash # and a newline \n"));
        assertTrue("Line 9 failed: " + testTable.get("string.9"), testTable.get("string.9").equals("this line should all be here with the hash #"));


    }

}
