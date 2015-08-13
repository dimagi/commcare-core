package org.commcare.util.test;

import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.util.mocks.LivePrototypeFactory;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.DataUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.fail;


public class CasePurgeFilterTests {

    Case a,b,c,d,e;
    DummyIndexedStorageUtility<Case> storage;
    String owner;
    String groupOwner;
    String otherOwner;
    Vector<String> groupOwned;
    Vector<String> userOwned;


    @Before
    public void setUp() throws Exception {

        storage =  new DummyIndexedStorageUtility<Case>(Case.class, new LivePrototypeFactory());

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

    @Test
    public void testOwnerPurge() {
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

    @Test
    public void testUnownedPurge() {
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

    @Test
    public void testOwnerLiveness() {
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

    @Test
    public void testGroupOwned() {
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

    @Test
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

    @Test
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

    @Test
    public void testDependenceDirection() {
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testExtensionRetention() {
        e.setIndex(new CaseIndex("a", "a", a.getCaseId(), CaseIndex.RELATIONSHIP_EXTENSION));

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

    @Test
    public void testExtensionPurge() {
        e.setIndex(new CaseIndex("a", "a", a.getCaseId(), CaseIndex.RELATIONSHIP_EXTENSION));
        a.setClosed(true);

        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);
            storage.write(e);

            int[] present = new int[] {c.getID(), b.getID(), d.getID()};
            int[] toRemove = new int[] {a.getID(), e.getID()};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    @Test
    public void testExtensionSubcaseChain() {
        e.setIndex(new CaseIndex("a", "a", a.getCaseId(), CaseIndex.RELATIONSHIP_EXTENSION));
        a.setClosed(true);

        b.setIndex(new CaseIndex("e", "e", e.getCaseId(), CaseIndex.RELATIONSHIP_CHILD));

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

    @Test
    public void testExtensionSubcaseChainClosed() {
        e.setIndex(new CaseIndex("a", "a", a.getCaseId(), CaseIndex.RELATIONSHIP_EXTENSION));
        a.setClosed(true);

        b.setIndex(new CaseIndex("e", "e", e.getCaseId(), CaseIndex.RELATIONSHIP_CHILD));
        b.setClosed(true);

        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);
            storage.write(e);

            int[] present = new int[] {c.getID(), d.getID()};
            int[] toRemove = new int[] {a.getID(), b.getID(), e.getID()};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    @Test
    public void testDoubleExtension() {
        e.setIndex(new CaseIndex("a", "a", a.getCaseId(), CaseIndex.RELATIONSHIP_EXTENSION));
        a.setClosed(true);

        c.setIndex(new CaseIndex("e", "e", e.getCaseId(), CaseIndex.RELATIONSHIP_EXTENSION));

        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);
            storage.write(e);

            int[] present = new int[] {b.getID(), d.getID()};
            int[] toRemove = new int[] {a.getID(), c.getID(), e.getID()};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    @Test
    public void testHeterogeneousChildren() {
        e.setIndex(new CaseIndex("a", "a", a.getCaseId(), CaseIndex.RELATIONSHIP_EXTENSION));
        a.setClosed(true);

        b.setIndex(new CaseIndex("a", "a", a.getCaseId(), CaseIndex.RELATIONSHIP_CHILD));

        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);
            storage.write(e);

            int[] present = new int[] {a.getID(), b.getID(), c.getID(), d.getID(), e.getID()};
            int[] toRemove = new int[] {};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    @Test
    public void testHeterogeneousParentage() {
        e.setIndex(new CaseIndex("a", "a", a.getCaseId(), CaseIndex.RELATIONSHIP_EXTENSION));
        a.setClosed(true);

        e.setIndex(new CaseIndex("b", "b", b.getCaseId(), CaseIndex.RELATIONSHIP_CHILD));

        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);
            storage.write(e);

            int[] present = new int[] {b.getID(), c.getID(), d.getID()};
            int[] toRemove = new int[] {a.getID(), e.getID()};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    @Test
    public void testDoubleIndex() {
        a.setClosed(true);

        e.setIndex(new CaseIndex("a_c", "a", a.getCaseId(), CaseIndex.RELATIONSHIP_CHILD));
        e.setIndex(new CaseIndex("a_e", "a", a.getCaseId(), CaseIndex.RELATIONSHIP_EXTENSION));


        try {
            storage.write(a);
            storage.write(b);
            storage.write(c);
            storage.write(d);
            storage.write(e);

            int[] present = new int[] {a.getID(), e.getID(), b.getID(), c.getID(), d.getID()};
            int[] toRemove = new int[] {};

            Vector<Integer> removed = storage.removeAll(new CasePurgeFilter(storage));
            testOutcome(storage, present, toRemove);
            testRemovedClaim(removed, toRemove);

        } catch(Exception e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }

    public void testOutcome(IStorageUtility<Case> storage, int[] p, int[] g) {
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
