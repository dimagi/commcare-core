package org.commcare.cases.entity;

import org.commcare.modern.util.Pair;
import org.commcare.util.EntitySortUtil;
import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Filter entity list via all string-representable entity fields
 */
public class EntityStringFilterer {
    private final String[] searchTerms;
    private final ArrayList<Pair<Integer, Integer>> matchScores = new ArrayList<>();

    private final NodeEntityFactory nodeFactory;
    protected final List<Entity<TreeReference>> matchList;
    protected final List<Entity<TreeReference>> fullEntityList;
    private final boolean isFuzzySearchEnabled;

    public EntityStringFilterer(String[] searchTerms,
            NodeEntityFactory nodeFactory,
            List<Entity<TreeReference>> fullEntityList,
            boolean isFuzzySearchEnabled) {
        this.isFuzzySearchEnabled = isFuzzySearchEnabled;
        this.matchList = new ArrayList<>();
        this.nodeFactory = nodeFactory;
        this.fullEntityList = fullEntityList;
        this.searchTerms = searchTerms;
        if (searchTerms == null || searchTerms.length == 0) {
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
        EntitySortUtil.sortEntities(fullEntityList,
                searchTerms,
                currentLocale,
                isFuzzySearchEnabled,
                matchScores,
                matchList,
                fullEntityList::get);
        return matchList;
    }
}
