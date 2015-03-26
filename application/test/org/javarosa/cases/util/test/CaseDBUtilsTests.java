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

package org.javarosa.cases.util.test;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

import org.commcare.cases.model.Case;
import org.commcare.cases.util.CaseDBUtils;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.MD5;

public class CaseDBUtilsTests extends TestCase {

    private static int NUM_TESTS = 3;

    private static final String one = "abc123";
    private static final String two = "123abc";

    /* (non-Javadoc)
     * @see j2meunit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    public CaseDBUtilsTests(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
    }

    public CaseDBUtilsTests(String name) {
        super(name);
    }

    public CaseDBUtilsTests() {
        super();
    }

    public Test suite() {
        TestSuite aSuite = new TestSuite();

        for (int i = 1; i <= NUM_TESTS; i++) {
            final int testID = i;

            aSuite.addTest(new CaseDBUtilsTests("DateData Test " + i, new TestMethod() {
                public void run (TestCase tc) {
                    ((CaseDBUtilsTests)tc).testMaster(testID);
                }
            }));
        }

        return aSuite;
    }
    public void testMaster (int testID) {
        //System.out.println("running " + testID);

        switch (testID) {
        case 1: testHighLevelStrategy(); break;
        case 2: testCaseDigest(); break;
        case 3: testOrderInvariance(); break;
        }
    }

    public void testHighLevelStrategy() {
        this.assertEquals("Case ID hashing method produces invalid result", MD5.toHex(CaseDBUtils.xordata(MD5.hash((one+":o").getBytes()), MD5.hash((two+":c").getBytes()))),"c4251c443d45aa2601bf16533fb9dbe1");
    }

    private Case[] genDummyCases() {
        Case[] ret = new Case[2];
        ret[0] = new Case("name","type");
        ret[0].setCaseId(one);
        ret[1] = new Case("name","type");
        ret[1].setCaseId(two);
        ret[1].setClosed(true);
        return ret;
    }

    public void testCaseDigest() {
        DummyIndexedStorageUtility<Case> testStorage =  new DummyIndexedStorageUtility<Case>(Case.class);
        Case[] dc = genDummyCases();
        try {
            testStorage.write(dc[0]);
            testStorage.write(dc[1]);
        } catch(Exception e){
            e.printStackTrace();
            this.fail("Exception when creating dummy storage for case digest test: " + e.getMessage());
        }

        this.assertEquals("Invalid state hash", CaseDBUtils.computeHash(testStorage),"c4251c443d45aa2601bf16533fb9dbe1");
    }

    public void testOrderInvariance() {
        DummyIndexedStorageUtility<Case> testStorage =  new DummyIndexedStorageUtility<Case>(Case.class);
        Case[] dc = genDummyCases();
        try {
            //NOTE: Depends on the implementation of the testStorage! badbadbad;
            testStorage.write(dc[1]);
            testStorage.write(dc[0]);
        } catch(Exception e){
            e.printStackTrace();
            this.fail("Exception when creating dummy storage for case digest invariance test: " + e.getMessage());
        }

        this.assertEquals("Invalid state hash on Inversion", CaseDBUtils.computeHash(testStorage),"c4251c443d45aa2601bf16533fb9dbe1");

    }
}
