package org.commcare.cases.query;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

/**
 * Created by ctsims on 12/14/2016.
 */

public class QueryCacheTests {
    QueryCacheHost grandparent, parent, child;

    String key = "key";
    String value = "value";

    @Before
    public void setup() {
        grandparent = new QueryCacheHost();
        parent = new QueryCacheHost(grandparent);
        child = new QueryCacheHost(parent);

    }

    @Test
    public void testGrandparentFetch() throws Exception {
        TestCache cache = grandparent.getQueryCache(TestCache.class);
        init(cache);

        Assert.assertEquals("Basic cache fetch", value, cache.hashmap.get(key));
        Assert.assertEquals("parent fetch", value, parent.getQueryCache(TestCache.class).hashmap.get(key));
        Assert.assertEquals("child fetch", value, child.getQueryCache(TestCache.class).hashmap.get(key));

    }


    @Test
    public void testParentFetch() throws Exception {
        TestCache cache = parent.getQueryCache(TestCache.class);
        init(cache);

        Assert.assertEquals("grandparent fetch", null, grandparent.getQueryCache(TestCache.class).hashmap.get(key));
        Assert.assertEquals("parent fetch", value, parent.getQueryCache(TestCache.class).hashmap.get(key));
        Assert.assertEquals("child fetch", value, child.getQueryCache(TestCache.class).hashmap.get(key));

    }

    @Test
    public void testChildFetch() throws Exception {
        TestCache cache = child.getQueryCache(TestCache.class);
        init(cache);

        Assert.assertEquals("grandparent fetch", null, grandparent.getQueryCache(TestCache.class).hashmap.get(key));
        Assert.assertEquals("parent fetch", null, parent.getQueryCache(TestCache.class).hashmap.get(key));
        Assert.assertEquals("child fetch", value, child.getQueryCache(TestCache.class).hashmap.get(key));

    }

    @Test
    public void testMultipleCaches() throws Exception {
        TestCache cache = parent.getQueryCache(TestCache.class);
        init(cache);

        TestCacheTwo cacheTwo = parent.getQueryCache(TestCacheTwo.class);
        cacheTwo.hashmap.put(key, "value2");

        Assert.assertEquals("grandparent fetch", null, grandparent.getQueryCache(TestCache.class).hashmap.get(key));
        Assert.assertEquals("parent fetch", "value2", parent.getQueryCache(TestCacheTwo.class).hashmap.get(key));
        Assert.assertEquals("child fetch", "value2", child.getQueryCache(TestCacheTwo.class).hashmap.get(key));

    }


    private void init(TestCache cache) {
        cache.hashmap.put(key, value);
    }

    public static class TestCache implements QueryCache {
        HashMap<String, String> hashmap = new HashMap<>();
    }

    public static class TestCacheTwo implements QueryCache {
        HashMap<String, String> hashmap = new HashMap<>();
    }

}
