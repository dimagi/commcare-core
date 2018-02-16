package org.commcare.cases.entity;

import org.commcare.modern.util.Pair;
import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


/**
 * Filter entity list via all string-representable entity fields
 */
public class EntityStringFilterer {
    private final String[] searchTerms;
    private final ArrayList<Pair<Integer, Integer>> matchScores = new ArrayList<>();

    private final NodeEntityFactory nodeFactory;
    protected final List<Entity<TreeReference>> matchList;
    protected final List<Entity<TreeReference>> fullEntityList;


    public EntityStringFilterer(String[] searchTerms,
                                NodeEntityFactory nodeFactory,
                                List<Entity<TreeReference>> fullEntityList) {
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
                    }
                }
            }
            if (add) {
                matchScores.add(Pair.create(index, score));
            }
        }

        matchList.addAll(matchScores.stream().map(match -> fullEntityList.get(match.first)).collect(Collectors.toList()));
        return matchList;
    }
}
