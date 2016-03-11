package org.commcare.util.cli;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.javarosa.core.model.condition.EvaluationContext;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * An entity detail subscreen displays one of the detail screens associated with an
 * entity list (or the only one if there is only one).
 *
 * It also provides navigation cues for switching which detail screen (or 'tab') is being
 * viewed.
 *
 * Created by ctsims on 8/20/2015.
 */
public class EntityDetailSubscreen extends Subscreen<EntityScreen> {
    private final int SCREEN_WIDTH = 100;

    private final String[] rows;
    private final String[] mDetailListTitles;

    private final int mCurrentIndex;

    public EntityDetailSubscreen(int currentIndex, Detail detail, EvaluationContext subContext, String[] detailListTitles) {
        DetailField[] fields = detail.getFields();
        rows = new String[fields.length];

        detail.populateEvaluationContextVariables(subContext);

        for (int i = 0; i < fields.length; ++i) {
            rows[i] = createRow(fields[i], subContext);
        }
        mDetailListTitles = detailListTitles;

        mCurrentIndex = currentIndex;
    }

    private String createRow(DetailField field, EvaluationContext ec) {
        StringBuilder row = new StringBuilder();
        String header = field.getHeader().evaluate(ec);

        CliUtils.addPaddedStringToBuilder(row, header, SCREEN_WIDTH / 2);
        row.append(" | ");

        String value;
        Object o = field.getTemplate().evaluate(ec);
        if (!(o instanceof String)) {
            value = "{ " + field.getTemplateForm() + " data}";
        } else {
            value = (String)o;
        }
        CliUtils.addPaddedStringToBuilder(row, value, SCREEN_WIDTH / 2);

        return row.toString();
    }

    @Override
    public void prompt(PrintStream out) {
        boolean multipleInputs = false;
        if (mDetailListTitles.length > 1) {
            createTabHeader(out);
            out.println("==============================================================================================");
            multipleInputs = true;
        }

        for (int i = 0; i < rows.length; ++i) {
            String row = rows[i];
            out.println(row);
        }

        String msg;
        if (multipleInputs) {
            msg = "Press enter to select this case, or the number of the detail tab to view";
        } else {
            msg = "Press enter to select this case";
        }
        out.println();
        out.println(msg);
    }

    @Override
    public String[] getOptions() {
        System.out.println("Entity Detail get options: " + Arrays.toString(rows));
        return rows;
    }

    private void createTabHeader(PrintStream out) {
        StringBuilder sb = new StringBuilder();
        int widthPerTab = (int)(SCREEN_WIDTH * 1.0 / mDetailListTitles.length);
        for (int i = 0; i < mDetailListTitles.length; ++i) {
            String title = i + ") " + mDetailListTitles[i];
            if (i == this.mCurrentIndex) {
                title = "[" + title + "]";
            }
            CliUtils.addPaddedStringToBuilder(sb, title, widthPerTab);
        }
        out.println(sb.toString());
    }


    @Override
    public boolean handleInputAndUpdateHost(String input, EntityScreen host) throws CommCareSessionException {
        System.out.println("Entity Detail handle input: " + input);
        if (input.trim().equals("")) {
            return true;
        }
        try {
            int i = Integer.parseInt(input);
            if (i >= 0 && i < mDetailListTitles.length) {
                host.setCurrentScreenToDetail(i);
                return false;
            }
        } catch (NumberFormatException e) {
            //This will result in things just executing again, which is fine.
        }
        return false;
    }

    public int getCurrentIndex(){
        return mCurrentIndex;
    }
}
