package org.javarosa.core.storage;

import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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
    Shoe[] fiveSortedSizesOfMensAdidas;

    Shoe[] combinedCollection;

    protected abstract IStorageUtilityIndexed<Shoe> createStorageUtility();

    @Before
    public void setupStorageContainer() {
        storage = createStorageUtility();

        nike = new Shoe("nike", "mens", 10);

        tenSizesOfMensNikes = new Shoe[10];
        for (int i = 0; i < 10; ++i) {
            tenSizesOfMensNikes[i] =
                    new Shoe("nike", "mens", i + 10);
        }

        eightSizesOfWomensNikes = new Shoe[8];
        for (int i = 0; i < 8; ++i) {
            eightSizesOfWomensNikes[i] =
                    new Shoe("nike", "womens", i + 20);
        }

        fiveSizesOfMensVans = new Shoe[5];
        for (int i = 0; i < 5; ++i) {
            fiveSizesOfMensVans[i] =
                    new Shoe("vans", "mens", i + 5);
        }

        fiveSortedSizesOfMensAdidas = new Shoe[5];
        for (int i = 0; i < 5; ++i) {
            fiveSortedSizesOfMensAdidas[i]=  new Shoe("adidas", "mens", 5 - i);
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
        assertEquals("Incorrect record read from DB", nike, shouldBeNike);
    }

    @Test
    public void testOverwrite() {
        String review = "This shoe is fly";
        storage.write(nike);
        assert nike.getReviewText().equals("");
        nike.setReview(review);
        storage.write(nike);

        Shoe shouldBeNewNike = storage.read(nike.getID());

        assertEquals("Persistable was not ovewritten correctly", review, shouldBeNewNike.getReviewText());
    }

    @Test
    public void testSingleMatch() {
        writeBulkSets();

        Set<Integer> sizeMatch = new HashSet<>();
        sizeMatch.add(fiveSortedSizesOfMensAdidas[2].getID());

        List<Integer> matches =
                storage.getIDsForValue(Shoe.META_SIZE, 3);

        assertEquals("Failed single index match [size][3]", sizeMatch, new HashSet<>(matches));

        List<Integer> matchesOnVector =
                storage.getIDsForValues(new String[]{Shoe.META_SIZE}, new Integer[]{3});

        assertEquals("Failed single vector index match [size][3]", sizeMatch, new HashSet<>(matchesOnVector));

    }

    @Test
    public void testBulkMetaMatching() {
        writeBulkSets();

        List<Integer> matches = storage.getIDsForValues(new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"});

        assertEquals("Failed index match [brand,style][nike,mens]", getIdsFromModels(tenSizesOfMensNikes), new HashSet<>(matches));

        LinkedHashSet<Integer> newResultPath = new LinkedHashSet<>();
        storage.getIDsForValues(new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"}, newResultPath);

        assertEquals("Failed index match [brand,style][nike,mens]", new HashSet<>(matches), newResultPath);


        Vector<Shoe> matchedRecords = storage.getRecordsForValues(new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"});
        assertEquals("Failed index match [brand,style][nike,mens]", getIdsFromModels(tenSizesOfMensNikes), getIdsFromModels(matchedRecords.toArray(new Shoe[]{})));
    }

    @Test
    public void testSortedRecords() {
        writeBulkSets();
        combineAllList();
        Vector<Shoe> matchedSortedRecords = storage.getSortedRecordsForValues(new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"adidas", "mens"},Shoe.META_SIZE+" DESC");
        Assert.assertArrayEquals("Failed index match [brand,style][adidas,mens]", fiveSortedSizesOfMensAdidas,matchedSortedRecords.toArray());

        Vector<Shoe> matchedAscSortedRecords = storage.getSortedRecordsForValues(new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"},Shoe.META_SIZE+" ASC");
        Assert.assertArrayEquals("Failed index match [brand,style][adidas,mens]", matchedAscSortedRecords.toArray(),tenSizesOfMensNikes);

        Vector<Shoe> matchedSortedRecordsEmptyValue = storage.getSortedRecordsForValues(new String[]{}, new String[]{},Shoe.META_SIZE+" DESC");
        Assert.assertArrayEquals("Failed index match [brand,style][adidas,mens]", matchedSortedRecordsEmptyValue.toArray(),combinedCollection);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Vector<Shoe> matchedDescSortedRecordsWithoutKey = storage.getSortedRecordsForValues(new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"adidas", "mens"}, " DESC");
        });
        Assert.assertEquals("Invalid format", exception.getMessage());
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            Vector<Shoe> matchedAscSortedRecordsTest = storage.getSortedRecordsForValues(new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"vans", "mens"}, Shoe.META_SIZE + " ASSC");
        });
        Assert.assertEquals("Invalid format", exception2.getMessage());
    }

    @Test
    public void testReadingAllEntries() {
        writeBulkSets();
        List<Integer> matches = storage.getIDsForValues(new String[0], new String[0]);
        Set<Integer> expectedMatches = getIdsFromModels(tenSizesOfMensNikes);
        expectedMatches.addAll(getIdsFromModels(eightSizesOfWomensNikes));
        expectedMatches.addAll(getIdsFromModels(fiveSizesOfMensVans));
        expectedMatches.addAll(getIdsFromModels(fiveSortedSizesOfMensAdidas));
        assertEquals("Failed index match for all entries", expectedMatches, new HashSet<>(matches));
    }

    private void writeBulkSets() {
        writeAll(tenSizesOfMensNikes);
        writeAll(eightSizesOfWomensNikes);
        writeAll(fiveSizesOfMensVans);
        writeAll(fiveSortedSizesOfMensAdidas);
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

    void combineAllList(){
        List<Shoe> shoeList = new ArrayList<>();

        Collections.addAll(shoeList, eightSizesOfWomensNikes);
        Collections.addAll(shoeList, tenSizesOfMensNikes);
        Collections.addAll(shoeList, fiveSortedSizesOfMensAdidas);
        Collections.addAll(shoeList, fiveSizesOfMensVans);
        Collections.sort(shoeList, new Comparator<Shoe>() {
            @Override
            public int compare(Shoe record1, Shoe record2) {
                try {

                        int comparison = ((Comparable) record2.size).compareTo(record1.size);
                        return comparison;
                } catch (Exception ignore) {
                }
                return 0; // Default to no ordering if field access fails
            }
        });
        combinedCollection = shoeList.toArray(new Shoe[0]);




    }

}
