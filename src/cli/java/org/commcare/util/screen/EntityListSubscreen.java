package org.commcare.util.screen;

import static org.commcare.util.screen.MultiSelectEntityScreen.USE_SELECTED_VALUES;

import org.commcare.cases.entity.Entity;
import org.commcare.cases.entity.EntitySortNotificationInterface;
import org.commcare.cases.entity.EntitySorter;
import org.commcare.cases.entity.EntityStringFilterer;
import org.commcare.cases.entity.NodeEntityFactory;
import org.commcare.modern.util.Pair;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.AccumulatingReporter;
import org.javarosa.core.model.trace.ReducingTraceReporter;
import org.javarosa.core.util.DataUtil;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * The entity list subscreen handles actually displaying the list of dynamic entities to the
 * user to be displayed and chosen during a <datum> selection
 *
 * Created by ctsims on 8/20/2015.
 */
public class EntityListSubscreen extends Subscreen<EntityScreen> {

    private static final int SCREEN_WIDTH = 100;

    private final TreeReference[] entitiesRefs;
    private final EntityScreenContext entityScreenContext;
    private String[] rows;
    private final String mHeader;

    private final Vector<Action> actions;

    private final Detail shortDetail;
    private final EvaluationContext rootContext;

    private boolean handleCaseIndex;
    private List<Entity<TreeReference>> entities;

    public EntityListSubscreen(Detail shortDetail, Vector<TreeReference> references, EvaluationContext context,
            boolean handleCaseIndex, EntityScreenContext entityScreenContext) throws CommCareSessionException {
        mHeader = createHeader(shortDetail, context);
        this.shortDetail = shortDetail;
        this.rootContext = context;
        this.entitiesRefs = new TreeReference[references.size()];
        this.handleCaseIndex = handleCaseIndex;
        this.entityScreenContext = entityScreenContext;
        references.copyInto(entitiesRefs);
        actions = shortDetail.getCustomActions(context);
        initEntities(context, shortDetail, entityScreenContext);
    }

    private void initEntities(EvaluationContext context, Detail shortDetail,
            EntityScreenContext entityScreenContext) {
        NodeEntityFactory nodeEntityFactory = new NodeEntityFactory(shortDetail, context);
        entities = new ArrayList<>();
        for (TreeReference reference : entitiesRefs) {
            entities.add(nodeEntityFactory.getEntity(reference));
        }
        nodeEntityFactory.prepareEntities(entities);
        filterEntities(entityScreenContext, nodeEntityFactory);
        sortEntities(entityScreenContext);
    }

    private void filterEntities(EntityScreenContext entityScreenContext, NodeEntityFactory nodeEntityFactory) {
        String searchText = entityScreenContext.getSearchText();
        boolean isFuzzySearchEnabled = entityScreenContext.isFuzzySearch();
        if (searchText != null && !"".equals(searchText)) {
            EntityStringFilterer filterer = new EntityStringFilterer(searchText.split(" "),
                    nodeEntityFactory, entities, isFuzzySearchEnabled);
            entities = filterer.buildMatchList();
        }
    }

    private void sortEntities(EntityScreenContext entityScreenContext) {
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
            for (int i = 0; i < shortDetail.getFields().length; ++i) {
                String header = shortDetail.getFields()[i].getHeader().evaluate();
                if (order.length == 0 && !"".equals(header)) {
                    order = new int[]{i};
                }
            }
        }
        java.util.Collections.sort(entities,
                new EntitySorter(shortDetail.getFields(), reverse, order, new LogNotifier()));
    }

    private String[] getRows(Detail detail) {
        String[] rows = new String[entities.size()];
        for (int e = 0; e < entities.size(); e++) {
            Entity<TreeReference> entity = entities.get(e);
            rows[e] = createRow(entity, detail);
        }
        return rows;
    }

    private String createRow(Entity<TreeReference> entity, Detail detail) {
        Object[] entityFields = entity.getData();
        DetailField[] detailFields = detail.getFields();
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < entityFields.length; i++) {
            Object entityField = entityFields[i];
            String s;
            if (!(entityField instanceof String)) {
                s = "";
            } else {
                s = (String)entityField;
            }
            row.append(ScreenUtils.pad(s, getWidthHint(detailFields, detailFields[i])));
        }
        return row.toString();
    }

    public static Pair<String[], int[]> getHeaders(Detail shortDetail, EvaluationContext context, int sortIndex) {
        DetailField[] fields = shortDetail.getFields();
        String[] headers = new String[fields.length];
        int[] widthHints = new int[fields.length];

        boolean reverse = sortIndex < 0;
        sortIndex = Math.abs(sortIndex) - 1;
        int[] sorts = shortDetail.getOrderedFieldIndicesForSorting();

        StringBuilder row = new StringBuilder();
        int i = 0;
        for (DetailField field : fields) {
            String s = field.getHeader().evaluate(context);

            int widthHint = getWidthHint(fields, field);
            ScreenUtils.addPaddedStringToBuilder(row, s, widthHint);

            if (DataUtil.intArrayContains(sorts, i)) {
                if (i == sortIndex) {
                    if (reverse) {
                        s = s + " Λ ";
                    } else {
                        s = s + " V ";
                    }
                }
            }

            headers[i] = s;
            widthHints[i] = widthHint;

            i++;
            if (i != fields.length) {
                row.append(" | ");
            }
        }
        return new Pair<>(headers, widthHints);
    }

    //So annoying how identical this is...
    private static String createHeader(Detail shortDetail, EvaluationContext context) {
        DetailField[] fields = shortDetail.getFields();

        StringBuilder row = new StringBuilder();
        int i = 0;
        for (DetailField field : fields) {
            String s = field.getHeader().evaluate(context);

            int widthHint = getWidthHint(fields, field);
            ScreenUtils.addPaddedStringToBuilder(row, s, widthHint);
            i++;
            if (i != fields.length) {
                row.append(" | ");
            }
        }
        return row.toString();
    }

    @Override
    public void prompt(PrintStream out) {
        int maxLength = String.valueOf(entitiesRefs.length).length();
        out.println(ScreenUtils.pad("", maxLength + 1) + mHeader);
        out.println("===========================================================================================");

        initRows();
        for (int i = 0; i < entitiesRefs.length; ++i) {
            String d = rows[i];
            out.println(ScreenUtils.pad(String.valueOf(i), maxLength) + ") " + d);
        }

        if (actions != null) {
            int actionCount = 0;
            for (Action action : actions) {
                out.println();
                out.println("action " + actionCount + ") " + action.getDisplay().evaluate().getName());
                actionCount += 1;
            }
        }
    }

    @Override
    public String[] getOptions() {
        initRows();
        return rows;
    }

    private void initRows() {
        if (rows == null) {
            rows = getRows(shortDetail);
        }
    }

    @Override
    public boolean handleInputAndUpdateHost(String input, EntityScreen host, boolean allowAutoLaunch,
            String[] selectedValues) throws CommCareSessionException {
        if (input.startsWith("action ") && actions != null) {
            int chosenActionIndex;
            try {
                chosenActionIndex = Integer.valueOf(input.substring("action ".length()).trim());
            } catch (NumberFormatException e) {
                return false;
            }
            if (actions.size() > chosenActionIndex) {
                host.setPendingAction(actions.elementAt(chosenActionIndex));
                return true;
            }
        }

        if (input.startsWith("debug ")) {
            String debugArg = input.substring("debug ".length());
            try {
                int chosenDebugIndex = Integer.valueOf(debugArg.trim());
                createRow(entities.get(chosenDebugIndex), shortDetail);
            } catch (NumberFormatException e) {
                if ("list".equals(debugArg)) {
                    host.printNodesetExpansionTrace(new AccumulatingReporter());
                }
            }
            return false;
        }

        if (input.startsWith("profile list")) {
            host.printNodesetExpansionTrace(new ReducingTraceReporter(false));
        }

        if (handleCaseIndex) {
            try {
                TreeReference[] selectedRefs;
                if (input.contentEquals(USE_SELECTED_VALUES)) {
                    if (selectedValues == null) {
                        throw new IllegalArgumentException("selected values can't be null");
                    }
                    selectedRefs = new TreeReference[selectedValues.length];
                    for (int i = 0; i < selectedValues.length; i++) {
                        int index = Integer.parseInt(selectedValues[i]);
                        selectedRefs[i] = entitiesRefs[index];
                    }
                } else {
                    int index = Integer.parseInt(input);
                    selectedRefs = new TreeReference[1];
                    selectedRefs[0] = entitiesRefs[index];
                }
                host.updateSelection(input, selectedRefs);
                return true;
            } catch (NumberFormatException e) {
                // This will result in things just executing again, which is fine.
            }
        } else {
            host.updateSelection(input, selectedValues);
            return true;
        }
        return false;
    }

    public Detail getShortDetail() {
        return shortDetail;
    }

    private static int getWidthHint(DetailField[] fields, DetailField field) {
        int widthHint = SCREEN_WIDTH / fields.length;
        try {
            widthHint = Integer.parseInt(field.getHeaderWidthHint());
        } catch (Exception e) {
            //Really don't care if it didn't work
        }
        return widthHint;
    }

    public Vector<Action> getActions() {
        return actions;
    }

    public List<Entity<TreeReference>> getEntities() {
        return entities;
    }

    private static class LogNotifier implements EntitySortNotificationInterface {
        @Override
        public void notifyBadFilter(String[] args) {

        }
    }
}
