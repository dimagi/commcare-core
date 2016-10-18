package org.commcare.util.cli;

import org.commcare.suite.model.Action;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
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

    public EntityListSubscreen(Detail shortDetail, Vector<TreeReference> references, EvaluationContext context) throws CommCareSessionException {
        mHeader = createHeader(shortDetail, context);

        rows = new String[references.size()];

        int i = 0;
        for (TreeReference entity : references) {
            rows[i] = createRow(entity, shortDetail, context);
            ++i;
        }

        this.mChoices = new TreeReference[references.size()];
        references.copyInto(mChoices);

        actions = shortDetail.getCustomActions(context);
    }

    private static String createRow(TreeReference entity, Detail shortDetail, EvaluationContext ec) {
        EvaluationContext context = new EvaluationContext(ec, entity);

        shortDetail.populateEvaluationContextVariables(context);

        DetailField[] fields = shortDetail.getFields();

        StringBuilder row = new StringBuilder();
        int i = 0;
        for (DetailField field : fields) {
            Object o;
            try {
                o = field.getTemplate().evaluate(context);
            } catch(XPathException e) {
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
            CliUtils.addPaddedStringToBuilder(row, s, widthHint);
            i++;
            if (i != fields.length) {
                row.append(" | ");
            }
        }
        return row.toString();
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
            CliUtils.addPaddedStringToBuilder(row, s, widthHint);
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
        out.println(CliUtils.pad("", maxLength + 1) + mHeader);
        out.println("==============================================================================================");

        for (int i = 0; i < mChoices.length; ++i) {
            String d = rows[i];
            out.println(CliUtils.pad(String.valueOf(i), maxLength) + ")" + d);
        }

        if(actions != null) {
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
    public boolean handleInputAndUpdateHost(String input, EntityScreen host) throws CommCareSessionException {
        if(input.startsWith("action ") && actions != null) {
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

        try {
            int i = Integer.parseInt(input);

            host.setHighlightedEntity(this.mChoices[i]);

            return !host.setCurrentScreenToDetail();
        } catch (NumberFormatException e) {
            //This will result in things just executing again, which is fine.
        }
        return false;
    }
}
