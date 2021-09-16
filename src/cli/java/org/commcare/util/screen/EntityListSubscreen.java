package org.commcare.util.screen;

import org.commcare.modern.util.Pair;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.AccumulatingReporter;
import org.javarosa.core.model.trace.EvaluationTraceReporter;
import org.javarosa.core.model.trace.ReducingTraceReporter;
import org.javarosa.core.model.utils.InstrumentationUtils;
import org.javarosa.core.util.DataUtil;
import org.javarosa.xpath.XPathException;

import java.io.PrintStream;
import java.util.Vector;

/**
 * The entity list subscreen handles actually displaying the list of dynamic entities to the
 * user to be displayed and chosen during a <datum> selection
 *
 * Created by ctsims on 8/20/2015.
 */
public class EntityListSubscreen extends Subscreen<EntityScreen> {

    private static final int SCREEN_WIDTH = 100;

    private final TreeReference[] mChoices;
    private final String[] rows;
    private final String mHeader;

    private final Vector<Action> actions;

    private final Detail shortDetail;
    private final EvaluationContext rootContext;

    private boolean handleCaseIndex;

    public EntityListSubscreen(Detail shortDetail, Vector<TreeReference> references, EvaluationContext context, boolean handleCaseIndex) throws CommCareSessionException {
        mHeader = createHeader(shortDetail, context);
        this.shortDetail = shortDetail;
        this.rootContext = context;
        this.mChoices = new TreeReference[references.size()];
        this.handleCaseIndex = handleCaseIndex;
        references.copyInto(mChoices);
        actions = shortDetail.getCustomActions(context);
        rows = getRows(mChoices, context, shortDetail);
    }

    public static String[] getRows(TreeReference[] references,
                                   EvaluationContext evaluationContext,
                                   Detail detail) {
        String[] rows = new String[references.length];
        int i = 0;
        for (TreeReference entity : references) {
            rows[i] = createRow(entity, evaluationContext, detail);
            ++i;
        }
        return rows;
    }

    private static String createRow(TreeReference entity, EvaluationContext evaluationContext, Detail detail) {
        return createRow(entity, false, evaluationContext, detail);
    }

    private static String createRow(TreeReference entity,
                                    boolean collectDebug,
                                    EvaluationContext evaluationContext,
                                    Detail detail) {
        EvaluationContext context = new EvaluationContext(evaluationContext, entity);
        EvaluationTraceReporter reporter = new AccumulatingReporter();

        if (collectDebug) {
            context.setDebugModeOn(reporter);
        }
        detail.populateEvaluationContextVariables(context);

        if (collectDebug) {
            InstrumentationUtils.printAndClearTraces(reporter, "Variable Traces");
        }

        DetailField[] fields = detail.getFields();

        StringBuilder row = new StringBuilder();
        XPathException detailFieldException = null;
        int i = 0;
        for (DetailField field : fields) {
            Object o;
            try {
                o = field.getTemplate().evaluate(context);
            } catch (XPathException e) {
                o = "error (see output)";
                if (detailFieldException == null) {
                    detailFieldException = e;
                }
            }
            String s;
            if (!(o instanceof String)) {
                s = "";
            } else {
                s = (String)o;
            }
            row.append(ScreenUtils.pad(s, getWidthHint(fields, field)));
        }

        if (detailFieldException != null) {
            detailFieldException.printStackTrace();
        }

        if (collectDebug) {
            InstrumentationUtils.printAndClearTraces(reporter, "Template Traces:");
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
        int maxLength = String.valueOf(mChoices.length).length();
        out.println(ScreenUtils.pad("", maxLength + 1) + mHeader);
        out.println("==============================================================================================");

        for (int i = 0; i < mChoices.length; ++i) {
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
        return rows;
    }

    @Override
    public boolean handleInputAndUpdateHost(String input, EntityScreen host, boolean allowAutoLaunch) throws CommCareSessionException {
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
        } else if (host.getAutoLaunchAction() != null && allowAutoLaunch) {
            host.setPendingAction(host.getAutoLaunchAction());
            return true;
        }

        if (input.startsWith("debug ")) {
            String debugArg = input.substring("debug ".length());
            try {
                int chosenDebugIndex = Integer.valueOf(debugArg.trim());
                createRow(this.mChoices[chosenDebugIndex], rootContext, shortDetail);
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
                int index = Integer.parseInt(input);
                host.setHighlightedEntity(mChoices[index]);
                // Set entity screen to show detail and redraw
                host.setCurrentScreenToDetail();
                return true;
            } catch (NumberFormatException e) {
                // This will result in things just executing again, which is fine.
                return false;
            }
        } else {
            host.setHighlightedEntity(input);
            // Set entity screen to show detail and redraw
            host.setCurrentScreenToDetail();
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

    
}
