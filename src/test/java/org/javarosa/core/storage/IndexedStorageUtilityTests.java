package org.javarosa.core.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.Interner;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;
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
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Stream;

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
        storage.removeAll();

        nike = new Shoe("nike", "mens", 10);

        tenSizesOfMensNikes = new Shoe[10];
        for (int i = 0; i < 10; ++i) {
            Random random = new Random();
            int randomNumber = random.nextInt(10) + 1;
            tenSizesOfMensNikes[i] =
                    new Shoe("nike", "mens", randomNumber);
        }

        eightSizesOfWomensNikes = new Shoe[8];
        for (int i = 0; i < 8; ++i) {
            eightSizesOfWomensNikes[i] =
                    new Shoe("nike", "womens", i + 1);
        }

        fiveSizesOfMensVans = new Shoe[5];
        for (int i = 0; i < 5; ++i) {
            fiveSizesOfMensVans[i] =
                    new Shoe("vans", "mens",i + 1);
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
        for (Shoe tenSizesOfMensNike : tenSizesOfMensNikes) {
            if(tenSizesOfMensNike.size == 3){
                sizeMatch.add(tenSizesOfMensNike.getID());
            }
        }
        sizeMatch.add(eightSizesOfWomensNikes[2].getID());
        sizeMatch.add(fiveSizesOfMensVans[2].getID());

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
    public void testReadingAllEntries() {
        writeBulkSets();
        List<Integer> matches = storage.getIDsForValues(new String[0], new String[0]);
        Set<Integer> expectedMatches = getIdsFromModels(tenSizesOfMensNikes);
        expectedMatches.addAll(getIdsFromModels(eightSizesOfWomensNikes));
        expectedMatches.addAll(getIdsFromModels(fiveSizesOfMensVans));
        assertEquals("Failed index match for all entries", expectedMatches, new HashSet<>(matches));
    }

    @Test
    public void testSortedRecords() {
        writeBulkSets();

        // checks Ascending order
        Vector<Shoe> sortedShoes = storage.getSortedRecordsForValues(
                new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"},
                Shoe.META_SIZE + " ASC");
        verifySort(sortedShoes);
        // verify all men nikes
        for (Shoe sortedShoe : sortedShoes) {
            assertEquals(sortedShoe.brand, "nike");
            assertEquals(sortedShoe.style, "mens");
        }

        Shoe[] sortedNikes = sortedShoes.toArray(new Shoe[0]);

        // checks Descending order
        sortedShoes = storage.getSortedRecordsForValues(
                new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"},
                Shoe.META_SIZE + " DESC");
        Assert.assertArrayEquals(sortedShoes.toArray(), reverseArray(sortedNikes));

        // orderby as null returns values as it is
        sortedShoes = storage.getSortedRecordsForValues(
                new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"}, null);
        Assert.assertArrayEquals(sortedShoes.toArray(), tenSizesOfMensNikes);

        // Incorrect keyword ASSC throws exception
        assertThrows(IllegalArgumentException.class, () -> storage.getSortedRecordsForValues(
                new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"},
                Shoe.META_SIZE + " ASSC"));

        // Checks sort with no selection
        sortedShoes = storage.getSortedRecordsForValues(new String[]{},
                new String[]{}, Shoe.META_SIZE + " ASC");
        verifySort(sortedShoes);
    }

    private void verifySort(Vector<Shoe> sortedShoes) {
        int lastSize = -1;
        for (Shoe sortedShoe : sortedShoes) {
            assertTrue(sortedShoe.size >= lastSize);
            lastSize = sortedShoe.size;
        }
    }

    private Shoe[] reverseArray(Shoe[] shoes) {
        ArrayList<Shoe> shoesList = new ArrayList<>(tenSizesOfMensNikes.length);
        for (int i = tenSizesOfMensNikes.length - 1; i >= 0; i--) {
            shoesList.add(shoes[i]);
        }
        return shoesList.toArray(new Shoe[]{});
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
