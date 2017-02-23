package org.commcare.cases.util.test;

import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.commcare.cases.util.CasePurgeFilter;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.fail;

public class CasePurgeFilterTests {

    private Case a;
    private Case b;
    private Case c;
    private Case d;
    private Case e;
    private DummyIndexedStorageUtility<Case> storage;
    private String owner;
    private String groupOwner;
    private Vector<String> groupOwned;
    private Vector<String> userOwned;

    @Before
    public void setUp() throws Exception {

        storage = new DummyIndexedStorageUtility<>(Case.class, new LivePrototypeFactory());

        owner ="owner";
        groupOwner = "groupowned";

        userOwned = new Vector<>();
        userOwned.addElement(owner);

        groupOwned = new Vector<>();
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

    public void testOutcome(IStorageUtilityIndexed<Case> storage, int[] p, int[] g) {
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
            fail("No case with index " + present.firstElement() + " in testdb");
        }
    }

    private void testRemovedClaim(Vector<Integer> removed, int[] toRemove) {
        if(removed.size() != toRemove.length) {
            fail("caseStorage purge returned incorrect size of returned items");
        }

        for(int i = 0 ; i < toRemove.length; ++i) {
            removed.removeElement(DataUtil.integer(toRemove[i]));
        }
        if(removed.size() > 0) {
            fail("caseStorage purge returned incorrect set of removed items");
        }
    }

    private Vector<Integer> atv(int[] a) {
        Vector<Integer> ret = new Vector<>(a.length);
        for(int i = 0; i < a.length ; ++i) {
            ret.addElement(DataUtil.integer(a[i]));
        }
        return ret;
    }
}
