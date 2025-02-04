package org.javarosa.core.storage;

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

        nike = new Shoe("nike", "mens", 10);

        tenSizesOfMensNikes = new Shoe[10];
        for (int i = 0; i < 10; ++i) {
            tenSizesOfMensNikes[i] =
                    new Shoe("nike", "mens", i + 1);
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
                storage.getIDsForValue(Shoe.META_SIZE, 3);

        Assert.assertEquals("Failed single index match [size][3]", sizeMatch, new HashSet<>(matches));

        List<Integer> matchesOnVector =
                storage.getIDsForValues(new String[]{Shoe.META_SIZE}, new Integer[]{3});

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
    public void testSortedRecords() {
        writeBulkSets();

        // checks Ascending order
        Vector<Shoe> sortedShoes = storage.getSortedRecordsForValues(
                new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"},
                Shoe.META_SIZE + " ASC");
        Assert.assertArrayEquals(sortedShoes.toArray(), tenSizesOfMensNikes);

        // checks Descending order
        sortedShoes = storage.getSortedRecordsForValues(
                new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"},
                Shoe.META_SIZE + " DESC");
        Assert.assertArrayEquals(sortedShoes.toArray(), reverseArray(tenSizesOfMensNikes));

        // Incorrect keyword ASSC throws exception
        assertThrows(IllegalArgumentException.class, () -> storage.getSortedRecordsForValues(
                new String[]{Shoe.META_BRAND, Shoe.META_STYLE}, new String[]{"nike", "mens"},
                Shoe.META_SIZE + " ASSC"));

        // Checks sort with no selection
        sortedShoes = storage.getSortedRecordsForValues(new String[]{},
                new String[]{}, Shoe.META_SIZE + " ASC");
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
