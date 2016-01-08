package org.commcare.suite.model;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Detail Fields represent the <field> elements of a suite's detail
 * definitions. The model contains the relevent text templates as well
 * as any layout or sorting options.
 *
 * @author ctsims
 */
public class DetailField implements Externalizable {

    public static final int DIRECTION_ASCENDING = 1;
    public static final int DIRECTION_DESCENDING = 2;

    /**
     * A special flag that signals that this "Sort" should actually be
     * a cached, asynchronous key
     */
    public static final int SORT_ORDER_CACHABLE = -2;

    private Text header;
    private DetailTemplate template; // Text or Graph
    private Text sort;
    private Text background;
    private String relevancy;
    private XPathExpression parsedRelevancy;
    private String headerWidthHint = null;  // Something like "500" or "10%"
    private String templateWidthHint = null;
    private String headerForm;
    private String templateForm;
    private int sortOrder = -1;
    private int sortDirection = DIRECTION_ASCENDING;
    private int sortType = Constants.DATATYPE_TEXT;
    private int gridX = -1;
    private int gridY = -1;
    private int gridWidth = -1;
    private int gridHeight = -1;
    private String horizontalAlign;
    private String verticalAlign;
    private String fontSize;
    private String cssID;

    public DetailField() {
    }

    /**
     * @return the header
     */
    public Text getHeader() {
        return header;
    }

    /**
     * @return the template
     */
    public DetailTemplate getTemplate() {
        return template;
    }

    /**
     * @return the sort
     */
    public Text getSort() {
        return sort;
    }

    /**
     * Determine if field should be shown, based on any relevancy condition.
     *
     * @param context Context in which to evaluate the field.
     * @return true iff the field should be displayed
     * @throws XPathSyntaxException
     */
    public boolean isRelevant(EvaluationContext context) throws XPathSyntaxException {
        if (relevancy == null) {
            return true;
        }

        if (parsedRelevancy == null) {
            parsedRelevancy = XPathParseTool.parseXPath(relevancy);
        }

        return XPathFuncExpr.toBoolean(parsedRelevancy.eval(context)).booleanValue();
    }

    /**
     * @return the headerWidthHint
     */
    public String getHeaderWidthHint() {
        return headerWidthHint;
    }

    /**
     * @return the templateHint
     */
    public String getTemplateWidthHint() {
        return templateWidthHint;
    }

    /**
     * @return the headerForm
     */
    public String getHeaderForm() {
        return headerForm;
    }

    /**
     * @return the templateForm
     */
    public String getTemplateForm() {
        return templateForm;
    }

    /**
     * @return the sortOrder
     */
    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * @return the sortDirection
     */
    public int getSortDirection() {
        return sortDirection;
    }

    public int getSortType() {
        return sortType;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        header = (Text)ExtUtil.read(in, Text.class, pf);
        template = (DetailTemplate)ExtUtil.read(in, new ExtWrapTagged(DetailTemplate.class), pf);
        sort = (Text)ExtUtil.read(in, new ExtWrapNullable(Text.class), pf);

        //Unfortunately I don't think there's a clean way to do this
        if (ExtUtil.readBool(in)) {
            relevancy = ExtUtil.readString(in);
        }
        headerWidthHint = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        templateWidthHint = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        headerForm = ExtUtil.readString(in);
        templateForm = ExtUtil.readString(in);
        sortOrder = ExtUtil.readInt(in);
        sortDirection = ExtUtil.readInt(in);
        sortType = ExtUtil.readInt(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, header);
        ExtUtil.write(out, new ExtWrapTagged(template));
        ExtUtil.write(out, new ExtWrapNullable(sort));

        boolean relevantSet = relevancy != null;
        ExtUtil.writeBool(out, relevantSet);
        if (relevantSet) {
            ExtUtil.writeString(out, relevancy);
        }
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(headerWidthHint));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(templateWidthHint));
        ExtUtil.writeString(out, headerForm);
        ExtUtil.writeString(out, templateForm);
        ExtUtil.writeNumeric(out, sortOrder);
        ExtUtil.writeNumeric(out, sortDirection);
        ExtUtil.writeNumeric(out, sortType);
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public String getHorizontalAlign() {
        return horizontalAlign;
    }

    public String getVerticalAlign() {
        return verticalAlign;
    }

    public String getFontSize() {
        return fontSize;
    }

    public String getCssId() {
        return cssID;
    }

    public Text getBackground() {
        return background;
    }

    public void setBackground(Text background) {
        this.background = background;
    }

    public static class Builder {
        final DetailField field;

        public Builder() {
            field = new DetailField();
        }

        public DetailField build() {
            return field;
        }

        /**
         * @param header the header to set
         */
        public void setHeader(Text header) {
            field.header = header;
        }

        /**
         * @param template the template to set
         */
        public void setTemplate(DetailTemplate template) {
            field.template = template;
        }

        /**
         * @param sort the sort to set
         */
        public void setSort(Text sort) {
            field.sort = sort;
        }

        /**
         * @param relevancy the relevancy to set
         */
        public void setRelevancy(String relevancy) {
            field.relevancy = relevancy;
        }

        public void setHeaderWidthHint(String hint) {
            field.headerWidthHint = hint;
        }

        public void setTemplateWidthHint(String hint) {
            field.templateWidthHint = hint;
        }

        /**
         * @param headerForm the headerForm to set
         */
        public void setHeaderForm(String headerForm) {
            field.headerForm = headerForm;
        }

        /**
         * @param templateForm the templateForm to set
         */
        public void setTemplateForm(String templateForm) {
            field.templateForm = templateForm;
        }

        /**
         * @param sortOrder the sortOrder to set
         */
        public void setSortOrder(int sortOrder) {
            field.sortOrder = sortOrder;
        }

        /**
         * @param sortDirection the sortDirection to set
         */
        public void setSortDirection(int sortDirection) {
            field.sortDirection = sortDirection;
        }

        public void setSortType(int sortType) {
            field.sortType = sortType;
        }

        public void setGridX(int gridX) {
            field.gridX = gridX;
        }

        public void setGridY(int gridY) {
            field.gridY = gridY;
        }

        public void setGridWidth(int gridWidth) {
            field.gridWidth = gridWidth;
        }

        public void setGridHeight(int gridHeight) {
            field.gridHeight = gridHeight;
        }

        public void setHorizontalAlign(String horizontalAlign) {
            field.horizontalAlign = horizontalAlign;
        }

        public void setVerticalAlign(String verticalAlign) {
            field.verticalAlign = verticalAlign;
        }

        public void setFontSize(String fontSize) {
            field.fontSize = fontSize;
        }

        public void setCssID(String id) {
            field.cssID = id;
        }

        public void setBackground(Text background) {
            field.background = background;
        }
    }
}
