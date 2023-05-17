package org.commcare.cases.query.queryset;

import java.util.*;

/**
 * A model query set implementation for queries which have one-to-many results.
 *
 * Created by skelly on 2023/05/17
 */

public class DualTableMultiMatchModelQuerySet implements ModelQuerySet {
    private Map<Integer, Set<Integer>> map = new HashMap<>();
    private LinkedHashSet<Integer> body = new LinkedHashSet<>();

    public void loadResult(Integer key, Integer value) {
        if (!map.containsKey(key)) {
            map.put(key, new HashSet<>());
        }
        map.get(key).add(value);
        body.add(value);
    }

    @Override
    public Collection<Integer> getMatchingValues(Integer i) {
        Set<Integer> result = map.get(i);
        if(result == null) {
            return null;
        }
        return new ArrayList<>(result);
    }

    @Override
    public LinkedHashSet<Integer> getSetBody() {
        return body;
    }
}
