package org.commcare.util.screen;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.Style;
import org.javarosa.core.model.condition.EvaluationContext;

import java.io.PrintStream;
import java.util.ArrayList;

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
    private final Object[] data ;
    private final String[] headers;
    private final Style[] styles;
    private final int mCurrentIndex;
    private Detail detail;

    public EntityDetailSubscreen(int currentIndex, Detail detail, EvaluationContext subContext, String[] detailListTitles) {
        this(currentIndex, detail, subContext, detailListTitles, false);
    }

    public EntityDetailSubscreen(int currentIndex, Detail detail, EvaluationContext subContext, String[] detailListTitles, boolean keepEmptyColumns) {
        this.detail = detail;
        DetailField[] fields = detail.getFields();

        ArrayList<String> rowTemporary = new ArrayList<>();
        ArrayList<String> headersTemporary = new ArrayList<>();
        ArrayList<Object> dataTemporary = new ArrayList<>();
        ArrayList<Style> stylesTemporary = new ArrayList<>();

        detail.populateEvaluationContextVariables(subContext);

        for (DetailField field : fields) {
            Object data = createData(field, subContext);
            // don't add empty details
            if (keepEmptyColumns || (data != null && !data.toString().trim().equals(""))) {
                dataTemporary.add(data);
                headersTemporary.add(createHeader(field, subContext));
                rowTemporary.add(createRow(field, subContext, data));
                stylesTemporary.add(createStyle(field));
            }
        }

        rows = new String[rowTemporary.size()];
        headers = new String[rowTemporary.size()];
        data = new Object[rowTemporary.size()];
        styles = new Style[rowTemporary.size()];

        rowTemporary.toArray(rows);
        headersTemporary.toArray(headers);
        dataTemporary.toArray(data);
        stylesTemporary.toArray(styles);

        mDetailListTitles = detailListTitles;
        mCurrentIndex = currentIndex;
    }

    private Style createStyle(DetailField field) {
        return new Style(field);
    }

    private String createHeader(DetailField field, EvaluationContext ec){return field.getHeader().evaluate(ec);}

    private Object createData(DetailField field, EvaluationContext ec){
        return field.getTemplate().evaluate(ec);
    }

    private String createRow(DetailField field, EvaluationContext ec, Object o) {
        StringBuilder row = new StringBuilder();
        String header = field.getHeader().evaluate(ec);

        ScreenUtils.addPaddedStringToBuilder(row, header, SCREEN_WIDTH / 2);
        row.append(" | ");

        String value;
        if (!(o instanceof String)) {
            value = "{ " + field.getTemplateForm() + " data}";
        } else {
            value = (String)o;
        }
        ScreenUtils.addPaddedStringToBuilder(row, value, SCREEN_WIDTH / 2);

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

        for (String row : rows) {
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
            ScreenUtils.addPaddedStringToBuilder(sb, title, widthPerTab);
        }
        out.println(sb.toString());
    }


    @Override
    public boolean handleInputAndUpdateHost(String input, EntityScreen host,
            boolean allowAutoLaunch, String[] selectedValues) throws CommCareSessionException {
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

    public Object[] getData() {
        return data;
    }

    public String[] getHeaders(){
        return headers;
    }

    public String[] getTitles() { return mDetailListTitles;}

    public Detail getDetail() {
        return detail;
    }

    public void setDetail(Detail detail) {
        this.detail = detail;
    }

    public Style[] getStyles() {
        return styles;
    }
}
