package org.commcare.cases.instance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.javarosa.core.model.instance.InstanceBase;
import org.junit.Test;


public class CaseInstanceTreeElementTest {

    @Test
    public void testGetStorageCacheName() {
        InstanceBase base = new InstanceBase("test");

        // No identifier — returns plain model name
        CaseInstanceTreeElement withoutId = new CaseInstanceTreeElement(base, null, null, null);
        assertEquals("casedb", withoutId.getStorageCacheName());

        // With identifier — returns model name + identifier
        CaseInstanceTreeElement withId = new CaseInstanceTreeElement(base, null, null, "search_table_1");
        assertEquals("casedb:search_table_1", withId.getStorageCacheName());

        // Two instances with different identifiers don't collide
        CaseInstanceTreeElement withId2 = new CaseInstanceTreeElement(base, null, null, "search_table_2");
        assertNotEquals(withId.getStorageCacheName(), withId2.getStorageCacheName());

        // Instance without identifier doesn't collide with one that has
        assertNotEquals(withoutId.getStorageCacheName(), withId.getStorageCacheName());

        // Legacy 2-arg constructor — same as null identifier
        CaseInstanceTreeElement legacy = new CaseInstanceTreeElement(base, null);
        assertEquals("casedb", legacy.getStorageCacheName());
    }
}