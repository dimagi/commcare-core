package org.javarosa.core.storage;

import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.Interner;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Tests for basic storage utility functions
 *
 * Created by ctsims on 9/22/2017.
 */

public abstract class IndexedStorageUtilityTests {

    IStorageUtilityIndexed<Shoe> storage;

    Shoe nike;

    Shoe[] tenSizesOfMensNikes;

    Shoe[] eightSizesOfWomensNikes;

    Shoe[] fiveSizesOfMensVans;

    protected abstract IStorageUtilityIndexed<Shoe> createStorageUtility();

    @Before
    public void setupStorageContainer() {
        storage = createStorageUtility();

        nike = new Shoe("nike", "mens", "10");

        tenSizesOfMensNikes = new Shoe[10];
        for (int i = 0; i < 10; ++i) {
            tenSizesOfMensNikes[i] =
                    new Shoe("nike", "mens", String.valueOf(i + 1));
        }

        eightSizesOfWomensNikes = new Shoe[8];
        for (int i = 0; i < 8; ++i) {
            eightSizesOfWomensNikes[i] =
                    new Shoe("nike", "womens", String.valueOf(i + 1));
        }

        fiveSizesOfMensVans = new Shoe[5];
        for (int i = 0; i < 5; ++i) {
            fiveSizesOfMensVans[i] =
                    new Shoe("vans", "mens", String.valueOf(i + 1));
        }
    }

    @Test
    public void ensureIdIssuance() {
        assert nike.getID() == -1;
        storage.write(nike);
        Assert.assertNotEquals("No ID issued to persistable", nike.getID(), -1);
    }

    @Test
    public void testWrite() {
        storage.write(nike);
        int id = nike.getID();
        Shoe shouldBeNike = storage.read(id);
        Assert.assertNotNull("Failed to read record from DB", shouldBeNike);
        Assert.assertEquals("Incorrect record read from DB", nike, shouldBeNike);
    }

    @Test
    public void testOverwrite() {
        String review = "This shoe is fly";
        storage.write(nike);
        assert nike.getReviewText().equals("");
        nike.setReview(review);
        storage.write(nike);

        Shoe shouldBeNewNike = storage.read(nike.getID());

        Assert.assertEquals("Persistable was not ovewritten correctly", review, shouldBeNewNike.getReviewText());
    }

    @Test
    public void testSingleMatch() {
        writeBulkSets();

        Set<Integer> sizeMatch = new HashSet<>();
        sizeMatch.add(tenSizesOfMensNikes[2].getID());
        sizeMatch.add(eightSizesOfWomensNikes[2].getID());
        sizeMatch.add(fiveSizesOfMensVans[2].getID());

        List<Integer> matches =
                storage.getIDsForValue(Shoe.META_SIZE, "3");

        Assert.assertEquals("Failed single index match [size][3]", sizeMatch, new HashSet<>(matches));

        List<Integer> matchesOnVector =
                storage.getIDsForValues(new String[]{Shoe.META_SIZE}, new String[]{"3"});

        Assert.assertEquals("Failed single vector index match [size][3]", sizeMatch, new HashSet<>(matchesOnVector));

    }

    @Test
    public void testBulkMetaMatching() {
        writeBulkSets();

        List<Integer> matches = storage.getIDsForValues(new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"});

        Assert.assertEquals("Failed index match [brand,style][nike,mens]", getIdsFromModels(tenSizesOfMensNikes), new HashSet<>(matches));

        LinkedHashSet<Integer> newResultPath = new LinkedHashSet<>();
        storage.getIDsForValues(new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"}, newResultPath);

        Assert.assertEquals("Failed index match [brand,style][nike,mens]", new HashSet<>(matches), newResultPath);


        Vector<Shoe> matchedRecords = storage.getRecordsForValues(new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"});
        Assert.assertEquals("Failed index match [brand,style][nike,mens]", getIdsFromModels(tenSizesOfMensNikes), getIdsFromModels(matchedRecords.toArray(new Shoe[]{})));
    }

    @Test
    public void testReadingAllEntries() {
        writeBulkSets();
        List<Integer> matches = storage.getIDsForValues(new String[0], new String[0]);
        Set<Integer> expectedMatches = getIdsFromModels(tenSizesOfMensNikes);
        expectedMatches.addAll(getIdsFromModels(eightSizesOfWomensNikes));
        expectedMatches.addAll(getIdsFromModels(fiveSizesOfMensVans));
        Assert.assertEquals("Failed index match for all entries", expectedMatches, new HashSet<>(matches));
    }

    @Test
    public void testGetBulkRecordsForIndex_multipleValues() {
        writeBulkSets();

        Set<Shoe> expected = new HashSet<>(Arrays.asList(
                tenSizesOfMensNikes[0], tenSizesOfMensNikes[1],
                eightSizesOfWomensNikes[0], eightSizesOfWomensNikes[1],
                fiveSizesOfMensVans[0], fiveSizesOfMensVans[1]
        ));

        Vector<Shoe> result = storage.getBulkRecordsForIndex(Shoe.META_SIZE, Arrays.asList("1", "2"));
        Assert.assertEquals("getBulkRecordsForIndex should return records for all matching values",
                expected, new HashSet<>(result));
    }

    @Test
    public void testGetBulkRecordsForIndex_singleValue() {
        writeBulkSets();

        Set<Shoe> expected = new HashSet<>(Arrays.asList(
                tenSizesOfMensNikes[4],
                eightSizesOfWomensNikes[4],
                fiveSizesOfMensVans[4]
        ));

        Vector<Shoe> result = storage.getBulkRecordsForIndex(Shoe.META_SIZE, Arrays.asList("5"));
        Assert.assertEquals("getBulkRecordsForIndex should return records matching the given value",
                expected, new HashSet<>(result));
    }

    @Test
    public void testGetBulkRecordsForIndex_noMatches() {
        writeBulkSets();

        Vector<Shoe> result = storage.getBulkRecordsForIndex(Shoe.META_BRAND, Arrays.asList("adidas"));
        Assert.assertTrue("getBulkRecordsForIndex should return empty result when no records match",
                result.isEmpty());
    }

    @Test
    public void testGetBulkRecordsForIndex_allValuesForField() {
        writeBulkSets();

        Set<Shoe> expected = new HashSet<>();
        expected.addAll(Arrays.asList(tenSizesOfMensNikes));
        expected.addAll(Arrays.asList(eightSizesOfWomensNikes));

        Vector<Shoe> result = storage.getBulkRecordsForIndex(Shoe.META_BRAND, Arrays.asList("nike"));
        Assert.assertEquals("getBulkRecordsForIndex should return all nike shoes",
                expected, new HashSet<>(result));
    }

    @Test
    public void testGetBulkIdsForIndex_multipleValues() {
        writeBulkSets();

        // Request IDs for sizes "1" and "2" — should match one shoe from each of the 3 sets
        Set<Integer> expected = new HashSet<>();
        expected.add(tenSizesOfMensNikes[0].getID());
        expected.add(tenSizesOfMensNikes[1].getID());
        expected.add(eightSizesOfWomensNikes[0].getID());
        expected.add(eightSizesOfWomensNikes[1].getID());
        expected.add(fiveSizesOfMensVans[0].getID());
        expected.add(fiveSizesOfMensVans[1].getID());

        Vector<Integer> result = storage.getBulkIdsForIndex(Shoe.META_SIZE, Arrays.asList("1", "2"));
        Assert.assertEquals("getBulkIdsForIndex should return IDs for all records matching any of the given values",
                expected, new HashSet<>(result));
    }

    @Test
    public void testGetBulkIdsForIndex_singleValue() {
        writeBulkSets();

        Set<Integer> expected = new HashSet<>();
        expected.add(tenSizesOfMensNikes[4].getID());
        expected.add(eightSizesOfWomensNikes[4].getID());
        expected.add(fiveSizesOfMensVans[4].getID());

        Vector<Integer> result = storage.getBulkIdsForIndex(Shoe.META_SIZE, Arrays.asList("5"));
        Assert.assertEquals("getBulkIdsForIndex should return IDs for all records matching the given value",
                expected, new HashSet<>(result));
    }

    @Test
    public void testGetBulkIdsForIndex_noMatches() {
        writeBulkSets();

        Vector<Integer> result = storage.getBulkIdsForIndex(Shoe.META_BRAND, Arrays.asList("adidas"));
        Assert.assertTrue("getBulkIdsForIndex should return empty result when no records match",
                result.isEmpty());
    }

    @Test
    public void testGetBulkIdsForIndex_allValuesForField() {
        writeBulkSets();

        Set<Integer> allIds = getIdsFromModels(tenSizesOfMensNikes);
        allIds.addAll(getIdsFromModels(eightSizesOfWomensNikes));

        Vector<Integer> result = storage.getBulkIdsForIndex(Shoe.META_BRAND, Arrays.asList("nike"));
        Assert.assertEquals("getBulkIdsForIndex should return IDs for all nike shoes",
                allIds, new HashSet<>(result));
    }

    private void writeBulkSets() {
        writeAll(tenSizesOfMensNikes);
        writeAll(eightSizesOfWomensNikes);
        writeAll(fiveSizesOfMensVans);
    }

    Set<Integer> getIdsFromModels(Shoe[] shoes) {
        Set<Integer> set = new HashSet<>();
        for (Shoe s : shoes) {
            set.add(s.getID());
        }
        return set;
    }

    private void writeAll(Shoe[] shoes) {
        for (Shoe s : shoes) {
            storage.write(s);
        }
    }

}
