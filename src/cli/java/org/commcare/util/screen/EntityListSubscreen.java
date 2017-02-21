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
    private final String mHeader;

    private final Vector<Action> actions;

    private final Detail shortDetail;
    private final EvaluationContext rootContext;

    public EntityListSubscreen(Detail shortDetail, Vector<TreeReference> references, EvaluationContext context) throws CommCareSessionException {
        mHeader = createHeader(shortDetail, context);
        this.shortDetail = shortDetail;
        this.rootContext = context;
        this.mChoices = new TreeReference[references.size()];
        references.copyInto(mChoices);
        actions = shortDetail.getCustomActions(context);
    }
    private String[] getRows(TreeReference[] references) {
        String[] rows = new String[references.length];
        int i = 0;
        for (TreeReference entity : references) {
            rows[i] = createRow(entity);
            ++i;
        }
        return rows;
    }

    private String createRow(TreeReference entity) {
        return createRow(entity, false);
    }

    private String createRow(TreeReference entity, boolean collectDebug) {
        EvaluationContext context = new EvaluationContext(rootContext, entity);
        EvaluationTraceReporter reporter = new AccumulatingReporter();

        if (collectDebug) {
            context.setDebugModeOn(reporter);
        }
        shortDetail.populateEvaluationContextVariables(context);

        if (collectDebug) {
            ScreenUtils.printAndClearTraces(reporter, "Variable Traces");
        }

        DetailField[] fields = shortDetail.getFields();

        StringBuilder row = new StringBuilder();
        int i = 0;
        for (DetailField field : fields) {
            Object o;
            try {
                o = field.getTemplate().evaluate(context);
            } catch (XPathException e) {
                o = "error (see output)";
                e.printStackTrace();
            }
            String s;
            if (!(o instanceof String)) {
                s = "";
            } else {
                s = (String)o;
            }

            int widthHint = SCREEN_WIDTH / fields.length;
            try {
                widthHint = Integer.parseInt(field.getTemplateWidthHint());
            } catch (Exception e) {
                //Really don't care if it didn't work
            }
            ScreenUtils.addPaddedStringToBuilder(row, s, widthHint);
            i++;
            if (i != fields.length) {
                row.append(" | ");
            }
        }

        if (collectDebug) {
            ScreenUtils.printAndClearTraces(reporter, "Template Traces:");
        }
        return row.toString();
    }

    public static Pair<String[], int[]> getHeaders(Detail shortDetail, EvaluationContext context){
        DetailField[] fields = shortDetail.getFields();
        String[] headers = new String[fields.length];
        int[] widthHints = new int[fields.length];

        StringBuilder row = new StringBuilder();
        int i = 0;
        for (DetailField field : fields) {
            String s = field.getHeader().evaluate(context);

            int widthHint = SCREEN_WIDTH / fields.length;
            try {
                widthHint = Integer.parseInt(field.getHeaderWidthHint());
            } catch (Exception e) {
                //Really don't care if it didn't work
            }
            ScreenUtils.addPaddedStringToBuilder(row, s, widthHint);

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

            int widthHint = SCREEN_WIDTH / fields.length;
            try {
                widthHint = Integer.parseInt(field.getHeaderWidthHint());
            } catch (Exception e) {
                //Really don't care if it didn't work
            }
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

        String[] rows = getRows(mChoices);

        for (int i = 0; i < mChoices.length; ++i) {
            String d = rows[i];
            out.println(ScreenUtils.pad(String.valueOf(i), maxLength) + ")" + d);
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
        return getRows(mChoices);
    }

    @Override
    public boolean handleInputAndUpdateHost(String input, EntityScreen host) throws CommCareSessionException {
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
                createRow(this.mChoices[chosenDebugIndex], true);
            } catch (NumberFormatException e) {
                if ("list".equals(debugArg)) {
                    host.printNodesetExpansionTrace(new AccumulatingReporter());
                }
            }
            return false;
        }

        if (input.startsWith("profile list")) {
            host.printNodesetExpansionTrace(new ReducingTraceReporter());
        }


        try {
            host.setHighlightedEntity(input);
            return true;
        } catch (NumberFormatException e) {
            //This will result in things just executing again, which is fine.
        }
        return false;
    }
}
