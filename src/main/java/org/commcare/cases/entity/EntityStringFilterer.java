package org.commcare.cases.entity;

import org.commcare.cases.util.StringUtils;
import org.commcare.modern.util.Pair;
import org.commcare.util.EntitySortUtil;
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
        EntitySortUtil.sortEntities(fullEntityList,
                searchTerms,
                currentLocale,
                isFuzzySearchEnabled,
                matchScores,
                matchList,
                new EntitySortUtil.EntitySortCallbackListener() {
                    @Override
                    protected Entity<TreeReference> getEntity(int index) {
                        return fullEntityList.get(index);
                    }
                });
        return matchList;
    }
}
