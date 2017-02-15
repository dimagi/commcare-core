package org.commcare.test.utilities;

import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.commcare.cases.util.CasePurgeFilter;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

/**
 * A class for running tests on the case purge logic.
 *
 * Reads external JSON documents containing preconditions setting up case logic, and then
 * validates that the resulting case database is consistent with the purge/sync logic.
 *
 * Created by ctsims on 10/13/2015.
 */
@RunWith(Parameterized.class)
public class CasePurgeTest {

    // TODO: START HERE
    @Parameterized.Parameters
    public static Iterable<JSONObject> testData() {
        try {
            JSONArray fullTestResource = new JSONArray(TestHelpers.getResourceAsString("/case_relationship_tests.json"));
            List<JSONObject> parameterSet = new ArrayList<>();
            for (int i = 0; i < fullTestResource.length(); ++i) {
                parameterSet.add(fullTestResource.getJSONObject(i));
            }
            return parameterSet;
        } catch (IOException | JSONException e) {
            RuntimeException failure =
                    new RuntimeException("Failed to parse input for CasePurgeTest");
            failure.initCause(e);
            throw failure;
        }
    }

    private final String name;
    private final HashSet<String> allCases = new HashSet<>();
    private final HashSet<String> ownedCases = new HashSet<>();
    private final HashSet<String> closedCases = new HashSet<>();
    private final HashSet<String> outcomeSet = new HashSet<>();
    private final ArrayList<String[]> indices = new ArrayList<>();

    public CasePurgeTest(JSONObject root) {
        this.name = root.getString("name");
        parseOutTestObjects(root);
    }

    private void parseOutTestObjects(JSONObject root) {
        if (root.has("cases")) {
            getCases(root.getJSONArray("cases"), allCases);
        }
        if (root.has("owned")) {
            getCases(root.getJSONArray("owned"), ownedCases);
        }
        if (root.has("closed")) {
            getCases(root.getJSONArray("closed"), closedCases);
        }

        if (root.has("subcases")) {
            getIndices(root.getJSONArray("subcases"), indices, CaseIndex.RELATIONSHIP_CHILD);
        }
        if (root.has("extensions")) {
            getIndices(root.getJSONArray("extensions"), indices, CaseIndex.RELATIONSHIP_EXTENSION);
        }
        getCases(root.getJSONArray("outcome"), outcomeSet);
    }

    private void getCases(JSONArray owned, HashSet<String> target) throws JSONException {
        for (int i = 0; i < owned.length(); ++i) {
            String c = owned.getString(i);
            allCases.add(c);
            target.add(c);
        }
    }

    private void getIndices(JSONArray indices, ArrayList<String[]> indexSet,
                                   String indexType) throws JSONException {
        for (int i = 0; i < indices.length(); ++i) {
            JSONArray index = indices.getJSONArray(i);
            String c = index.getString(0);
            String target = index.getString(1);
            allCases.add(c);
            allCases.add(target);
            indexSet.add(new String[]{c, target, indexType});
        }
    }

    @Test
    public void executeTest() {
        DummyIndexedStorageUtility<Case> storage =
                new DummyIndexedStorageUtility<>(Case.class, new LivePrototypeFactory());

        String userId = "user";

        initCaseStorage(storage, userId);

        Vector<String> ownerIds = new Vector<>();
        ownerIds.add(userId);

        storage.removeAll(new CasePurgeFilter(storage, ownerIds));

        HashSet<String> inStorage = new HashSet<>();
        for (IStorageIterator<Case> iterator = storage.iterate(); iterator.hasMore(); ) {
            Case c = iterator.nextRecord();
            inStorage.add(c.getCaseId());
        }

        Assert.assertEquals(name, outcomeSet, inStorage);

    }

    private void initCaseStorage(DummyIndexedStorageUtility<Case> storage,
                                 String userId) {
        for (String c : allCases) {
            Case theCase = new Case(c, "purge_test_case");
            theCase.setCaseId(c);
            if (ownedCases.contains(c)) {
                theCase.setUserId(userId);
            }
            if (closedCases.contains(c)) {
                theCase.setClosed(true);
            }
            storage.write(theCase);
        }

        for (String[] index : indices) {
            Case theCase = storage.getRecordForValue(Case.INDEX_CASE_ID, index[0]);
            CaseIndex caseIndex =
                    new CaseIndex(index[0] + index[1] + index[2],
                            "purge_test_case", index[1], index[2]);
            theCase.setIndex(caseIndex);
            storage.write(theCase);
        }
    }

    public String getName() {
        return name;
    }
    
}
