package org.commcare.cases.entity;

import org.commcare.cases.util.StringUtils;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.instance.TreeReference;

import java.util.*;


/**
 * Filter entity list via all string-representable entity fields
 */
public class EntityStringFilterer {
    private final boolean isFilterEmpty;
    private final String[] searchTerms;
    private final ArrayList<Pair<Integer, Integer>> matchScores = new ArrayList<>();
    private final boolean isAsyncMode;
    private final boolean isFuzzySearchEnabled;

    private final NodeEntityFactory nodeFactory;
    //private final EntityListAdapter adapter;
    protected final List<Entity<TreeReference>> matchList;
    protected final List<Entity<TreeReference>> fullEntityList;


    public EntityStringFilterer(String[] searchTerms,
                                boolean isAsyncMode, boolean isFuzzySearchEnabled,
                                NodeEntityFactory nodeFactory,
                                List<Entity<TreeReference>> fullEntityList) {
        this.matchList = new ArrayList<>();
        this.nodeFactory = nodeFactory;
        this.fullEntityList = fullEntityList;
        this.isAsyncMode = isAsyncMode;
        this.isFuzzySearchEnabled = isFuzzySearchEnabled;
        this.isFilterEmpty = searchTerms == null || searchTerms.length == 0;
        this.searchTerms = searchTerms;
        if (isFilterEmpty) {
            matchList.addAll(fullEntityList);
        }
    }

    public List<Entity<TreeReference>> buildMatchList() {

        while (!nodeFactory.isEntitySetReady()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Locale currentLocale = Locale.getDefault();
        //It's a bit sketchy here, because this DB lock will prevent
        //anything else from processing
        for (int index = 0; index < fullEntityList.size(); ++index) {
            Entity<TreeReference> e = fullEntityList.get(index);
            boolean add = false;
            int score = 0;
            filter:
            for (String filter : searchTerms) {
                add = false;
                for (int i = 0; i < e.getNumFields(); ++i) {
                    String field = e.getNormalizedField(i).toLowerCase();
                    if (!"".equals(field) && field.toLowerCase(currentLocale).contains(filter.toLowerCase())) {
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
        if (isAsyncMode) {
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
        return matchList;
    }
}
