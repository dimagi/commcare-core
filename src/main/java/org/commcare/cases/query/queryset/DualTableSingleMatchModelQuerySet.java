package org.commcare.cases.query.queryset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * A model query set implementation for queries which are one-to-one results.
 *
 * Maintains the original order of arguments received to ensure that lookups happen
 * in a predictable manner (and short-term caches will maximize hits)
 *
 * Created by ctsims on 2/6/2017.
 */

public class DualTableSingleMatchModelQuerySet implements ModelQuerySet {
    private Map<Integer, Integer> map = new HashMap<>();
    private LinkedHashSet<Integer> body = new LinkedHashSet<>();

    public void loadResult(Integer key, Integer value) {
        map.put(key, value);
        body.add(value);
    }

    @Override
    public Collection<Integer> getMatchingValues(Integer i) {
        Integer result = map.get(i);
        if(result == null) {
            return null;
        }
        ArrayList<Integer> ret = new ArrayList<>();
        ret.add(result);
        return ret;
    }

    @Override
    public LinkedHashSet<Integer> getSetBody() {
        return body;
    }
}
