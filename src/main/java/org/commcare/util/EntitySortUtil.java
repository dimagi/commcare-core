package org.commcare.util;

import org.commcare.cases.entity.Entity;
import org.commcare.cases.util.StringUtils;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Created by willpride on 2/21/18.
 */

public class EntitySortUtil {

    public abstract static class EntitySortCallbackListener {
        protected abstract Entity<TreeReference> getEntity(int index);
    }

    public static void sortEntities(List<Entity<TreeReference>> fullEntityList,
                                    String[] searchTerms,
                                    Locale currentLocale,
                                    boolean isFuzzySearchEnabled,
                                    ArrayList<Pair<Integer, Integer>> matchScores,
                                    List<Entity<TreeReference>> matchList,
                                    EntitySortCallbackListener listener) {
        for (int index = 0; index < fullEntityList.size(); ++index) {

            //Every once and a while we should make sure we're not blocking anything with the database
            Entity<TreeReference> e = listener.getEntity(index);
            if (e == null) {
                return;
            }
            boolean add = false;
            int score = 0;
            filter:
            for (String filter : searchTerms) {
                add = false;
                for (int i = 0; i < e.getNumFields(); ++i) {
                    String field = e.getNormalizedField(i);
                    if (!"".equals(field) && field.toLowerCase(currentLocale).contains(filter)) {
                        add = true;
                        continue filter;
                    } else if (isFuzzySearchEnabled) {
                        // We possibly now want to test for edit distance for
                        // fuzzy matching
                        for (String fieldChunk : e.getSortFieldPieces(i)) {
                            Pair<Boolean, Integer> match = StringUtils.fuzzyMatch(filter, fieldChunk);
                            if (match.first) {
                                add = true;
                                score += match.second;
                                continue filter;
                            }
                        }
                    }
                }
                if (!add) {
                    break;
                }
            }
            if (add) {
                matchScores.add(Pair.create(index, score));
            }
        }
        // If fuzzy search is enabled need to re-sort based on edit distance
        if (isFuzzySearchEnabled) {
            Collections.sort(matchScores, new Comparator<Pair<Integer, Integer>>() {
                @Override
                public int compare(Pair<Integer, Integer> lhs, Pair<Integer, Integer> rhs) {
                    return lhs.second - rhs.second;
                }
            });
        }

        for (Pair<Integer, Integer> match : matchScores) {
            matchList.add(fullEntityList.get(match.first));
        }
    }
}
