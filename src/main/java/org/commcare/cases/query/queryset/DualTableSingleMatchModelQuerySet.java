package org.commcare.cases.query.queryset;

import org.commcare.cases.query.QueryContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by ctsims on 2/6/2017.
 */

public class DualTableSingleMatchModelQuerySet implements ModelQuerySet {
    Map<Integer, Integer> map = new HashMap<>();
    LinkedHashSet<Integer> body = new LinkedHashSet<>();

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
