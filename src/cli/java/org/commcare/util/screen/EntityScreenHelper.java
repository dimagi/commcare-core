package org.commcare.util.screen;

import org.commcare.cases.entity.Entity;
import org.commcare.cases.entity.EntitySortNotificationInterface;
import org.commcare.cases.entity.EntitySorter;
import org.commcare.cases.entity.EntityStringFilterer;
import org.commcare.cases.entity.NodeEntityFactory;
import org.commcare.suite.model.Detail;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Common methods for initialising entities
 */
public class EntityScreenHelper {

    /**
     * Initialises given entity references into Entity models
     * @param context evaluation context to calculate detail fields
     * @param detail detail definition to map the given entity references to
     * @param entityScreenContext entity screen context
     * @param entitiesRefs references to initialise
     * @return List of initialised entity models
     */
    public static List<Entity<TreeReference>> initEntities(EvaluationContext context, Detail detail,
            EntityScreenContext entityScreenContext, TreeReference[] entitiesRefs) {
        NodeEntityFactory nodeEntityFactory = new NodeEntityFactory(detail, context);
        List<Entity<TreeReference>> entities = new ArrayList<>();
        for (TreeReference reference : entitiesRefs) {
            entities.add(nodeEntityFactory.getEntity(reference));
        }
        nodeEntityFactory.prepareEntities(entities);
        entities = filterEntities(entityScreenContext, nodeEntityFactory, entities);
        return sortEntities(entityScreenContext, entities, detail);
    }

    private static List<Entity<TreeReference>> filterEntities(EntityScreenContext entityScreenContext, NodeEntityFactory nodeEntityFactory,
            List<Entity<TreeReference>> entities) {
        String searchText = entityScreenContext.getSearchText();
        boolean isFuzzySearchEnabled = entityScreenContext.isFuzzySearch();
        if (searchText != null && !"".equals(searchText)) {
            EntityStringFilterer filterer = new EntityStringFilterer(searchText.split(" "),
                    nodeEntityFactory, entities, isFuzzySearchEnabled);
            entities = filterer.buildMatchList();
        }
        return entities;
    }

    private static List<Entity<TreeReference>> sortEntities(EntityScreenContext entityScreenContext, List<Entity<TreeReference>> entities,
            Detail shortDetail) {
        int sortIndex = entityScreenContext.getSortIndex();
        int[] order;
        boolean reverse = false;
        if (sortIndex != 0) {
            if (sortIndex < 0) {
                reverse = true;
                sortIndex = Math.abs(sortIndex);
            }
            // sort index is one indexed so adjust for that
            int sortFieldIndex = sortIndex - 1;
            order = new int[]{sortFieldIndex};
        } else {
            order = shortDetail.getOrderedFieldIndicesForSorting();
            if (order.length == 0) {
                for (int i = 0; i < shortDetail.getFields().length; ++i) {
                    String header = shortDetail.getFields()[i].getHeader().evaluate();
                    if (!"".equals(header)) {
                        order = new int[]{i};
                        break;
                    }
                }
            }
        }
        java.util.Collections.sort(entities,
                new EntitySorter(shortDetail.getFields(), reverse, order, new LogNotifier()));
        return entities;
    }

    private static class LogNotifier implements EntitySortNotificationInterface {
        @Override
        public void notifyBadFilter(String[] args) {

        }
    }
}
