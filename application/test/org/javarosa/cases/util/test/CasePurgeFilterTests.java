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

import java.util.Vector;

import org.commcare.cases.model.Case;
import org.commcare.cases.util.CasePurgeFilter;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.DataUtil;

public class CasePurgeFilterTests extends TestCase {

    private static int NUM_TESTS = 11;

    Case a,b,c,d,e;
    DummyIndexedStorageUtility<Case> storage;
    String owner;
    String groupOwner;
    String otherOwner;
    Vector<String> groupOwned;
    Vector<String> userOwned;


    /* (non-Javadoc)
     * @see j2meunit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        storage =  new DummyIndexedStorageUtility<Case>(Case.class);

        owner ="owner";
        otherOwner = "otherowner";
        groupOwner = "groupowned";

        userOwned = new Vector<String>();
        userOwned.addElement(owner);

        groupOwned = new Vector<String>();
        groupOwned.addElement(owner);
        groupOwned.addElement(groupOwner);

        a = new Case("a","a");
        a.setCaseId("a");
        a.setUserId(owner);
        b = new Case("b","b");
        b.setCaseId("b");
        b.setUserId(owner);
        c = new Case("c","c");
        c.setCaseId("c");
        c.setUserId(owner);
        d = new Case("d","d");
        d.setCaseId("d");
        d.setUserId(owner);
        e = new Case("e","e");
        e.setCaseId("e");
        e.setUserId(groupOwner);
    }

    public CasePurgeFilterTests(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
    }

    public CasePurgeFilterTests(String name) {
        super(name);
    }

    public CasePurgeFilterTests() {
        super();
    }

    public Test suite() {
        TestSuite aSuite = new TestSuite();

        for (int i = 1; i <= NUM_TESTS; i++) {
            final int testID = i;

            aSuite.addTest(new CasePurgeFilterTests("DateData Test " + i, new TestMethod() {
                public void run (TestCase tc) {
                    ((CasePurgeFilterTests)tc).testMaster(testID);
                }
            }));
        }

        return aSuite;
    }
    public void testMaster (int testID) {
        //System.out.println("running " + testID);

        switch (testID) {
        case 1: testNoDependence(); break;
        case 2: testLiveDependency(); break;
        case 3: testDependenceDirection(); break;
        case 4: testDeadness(); break;
        case 5: testDoubleChain(); break;
        case 6: testAlternatingChain(); break;
        case 7: testLopsidedChain(); break;
        case 8: testOwnerPurge(); break;
        case 9: testUnownedPurge(); break;
        case 10: testOwnerLiveness(); break;
        case 11: testGroupOwned(); break;
        }
    }


    private void testOwnerPurge() {
        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);

            int[] present = new int[] {a.getID(), c.getID(), b.getID(), d.getID()};
            int[] toRemove = new int[] {  };

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage, userOwned));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    private void testUnownedPurge() {
        b.setUserId(otherOwner);
        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);

            int[] present = new int[] {a.getID(), c.getID(), d.getID()};
            int[] toRemove = new int[] { b.getID()};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage, userOwned));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    private void testOwnerLiveness() {
        b.setUserId(otherOwner);
        c.setUserId(otherOwner);

        d.setIndex("b","b",b.getCaseId());

        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);

            int[] present = new int[] {a.getID(), b.getID(), d.getID()};
            int[] toRemove = new int[] { c.getID()};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage, userOwned));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }



    private void testGroupOwned() {
        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);
            storage.write(e);

            int[] present = new int[] {a.getID(), c.getID(), d.getID(), b.getID(), e.getID()};
            int[] toRemove = new int[] {};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage, groupOwned));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }

    }

    public void testNoDependence() {
        b.setClosed(true);

        d.setClosed(true);
        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);

            int[] present = new int[] {a.getID(), c.getID()};
            int[] toRemove = new int[] { b.getID(), d.getID() };

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }


    public void testLiveDependency() {
        b.setClosed(true);
        d.setIndex("b", "b", b.getCaseId());

        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);

            int[] present = new int[] {a.getID(), b.getID(), c.getID(), d.getID()};
            int[] toRemove = new int[] { };

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    public void testDependenceDirection() {;
        d.setIndex("b", "b", b.getCaseId());
        d.setClosed(true);

        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);

            int[] present = new int[] {a.getID(), b.getID(), c.getID()};
            int[] toRemove = new int[] {d.getID()};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    public void testDeadness() {
        b.setClosed(true);
        d.setIndex("b", "b", b.getCaseId());
        d.setClosed(true);

        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);

            int[] present = new int[] {a.getID(), c.getID()};
            int[] toRemove = new int[] {b.getID(), d.getID()};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    public void testDoubleChain() {
        b.setClosed(true);
        d.setIndex("b", "b", b.getCaseId());
        d.setClosed(true);
        e.setIndex("d", "d", d.getCaseId());

        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);
            storage.write(e);

            int[] present = new int[] {a.getID(), c.getID(), b.getID(), d.getID(), e.getID()};
            int[] toRemove = new int[] {};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    public void testAlternatingChain() {
        b.setClosed(true);
        d.setIndex("b", "b", b.getCaseId());
        e.setIndex("d", "d", d.getCaseId());
        e.setClosed(true);

        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);
            storage.write(e);

            int[] present = new int[] {a.getID(), c.getID(), b.getID(), d.getID()};
            int[] toRemove = new int[] {e.getID()};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    public void testLopsidedChain() {
        d.setIndex("b", "b", b.getCaseId());
        d.setClosed(true);
        e.setIndex("d", "d", d.getCaseId());
        e.setClosed(true);

        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);
            storage.write(e);

            int[] present = new int[] {a.getID(), c.getID(), b.getID()};
            int[] toRemove = new int[] {d.getID(), e.getID()};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }


    private void testOutcome(IStorageUtility<Case> storage, int[] p, int[] g) {
        Vector<Integer> present = atv(p);
        Vector<Integer> gone = atv(g);

        for(IStorageIterator<Case> iterator = storage.iterate(); iterator.hasMore(); ) {
            Integer id = iterator.peekID();
            present.removeElement(id);
            if(gone.contains(id)) {
                fail("Case: " + iterator.nextRecord().getCaseId() + " not purged");
            }
            iterator.nextID();
        }
        if(present.size() > 0) {
            fail("No case with index " + present.firstElement().intValue() + " in testdb");
        }
    }

    private void testRemovedClaim(Vector<Integer> removed, int[] toRemove) {
        if(removed.size() != toRemove.length) {
            fail("storage purge returned incorrect size of returned items");
        }

        for(int i = 0 ; i < toRemove.length; ++i) {
            removed.removeElement(DataUtil.integer(toRemove[i]));
        }
        if(removed.size() > 0) {
            fail("storage purge returned incorrect set of removed items");
        }
    }

    private Vector<Integer> atv(int[] a) {
        Vector<Integer> ret = new Vector<Integer>(a.length);
        for(int i = 0; i < a.length ; ++i) {
            ret.addElement(DataUtil.integer(a[i]));
        }
        return ret;
    }
}
