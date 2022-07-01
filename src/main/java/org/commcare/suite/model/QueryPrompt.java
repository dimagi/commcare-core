package org.commcare.suite.model;

import org.javarosa.core.model.ItemsetBinding;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

// Model for <prompt> node in {@link
public class QueryPrompt implements Externalizable {

    public static final String INPUT_TYPE_SELECT1 = "select1";
    public static final String INPUT_TYPE_SELECT = "select";
    public static final String INPUT_TYPE_DATERANGE = "daterange";
    public static final String INPUT_TYPE_DATE = "date";
    public static final String INPUT_TYPE_ADDRESS = "address";

    private String key;

    @Nullable
    private String appearance;

    @Nullable
    private String input;

    @Nullable
    private String receive;

    @Nullable
    private String hidden;

    private DisplayUnit display;

    @Nullable
    private XPathExpression defaultValueExpr;

    @Nullable
    private ItemsetBinding itemsetBinding;

    @Nullable
    private XPathExpression exclude;

    @Nullable
    private XPathExpression required;

    @Nullable
    private String requiredMsg;

    private boolean allowBlankValue;

    @Nullable
    private QueryPromptValidation validation;

    @SuppressWarnings("unused")
    public QueryPrompt() {
    }

    public QueryPrompt(String key, String appearance, String input, String receive,
                       String hidden, DisplayUnit display, ItemsetBinding itemsetBinding, 
                       XPathExpression defaultValueExpr, boolean allowBlankValue, XPathExpression exclude,
                       XPathExpression required, String requiredMsg, QueryPromptValidation validation) {

        this.key = key;
        this.appearance = appearance;
        this.input = input;
        this.receive = receive;
        this.hidden = hidden;
        this.display = display;
        this.itemsetBinding = itemsetBinding;
        this.defaultValueExpr = defaultValueExpr;
        this.allowBlankValue = allowBlankValue;
        this.exclude = exclude;
        this.required = required;
        this.validation = validation;
        this.requiredMsg = requiredMsg;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        key = (String)ExtUtil.read(in, String.class, pf);
        appearance = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        input = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        receive = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        hidden = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        display = (DisplayUnit)ExtUtil.read(in, DisplayUnit.class, pf);
        itemsetBinding = (ItemsetBinding)ExtUtil.read(in, new ExtWrapNullable(ItemsetBinding.class), pf);
        defaultValueExpr = (XPathExpression)ExtUtil.read(in, new ExtWrapNullable(new ExtWrapTagged()), pf);
        allowBlankValue = ExtUtil.readBool(in);
        exclude = (XPathExpression)ExtUtil.read(in, new ExtWrapNullable(new ExtWrapTagged()), pf);
        required = (XPathExpression)ExtUtil.read(in, new ExtWrapNullable(new ExtWrapTagged()), pf);
        requiredMsg = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        validation = (QueryPromptValidation)ExtUtil.read(in, new ExtWrapNullable(QueryPromptValidation.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, key);
        ExtUtil.write(out, new ExtWrapNullable(appearance));
        ExtUtil.write(out, new ExtWrapNullable(input));
        ExtUtil.write(out, new ExtWrapNullable(receive));
        ExtUtil.write(out, new ExtWrapNullable(hidden));
        ExtUtil.write(out, display);
        ExtUtil.write(out, new ExtWrapNullable(itemsetBinding));
        ExtUtil.write(out, new ExtWrapNullable(defaultValueExpr == null ? null : new ExtWrapTagged(defaultValueExpr)));
        ExtUtil.writeBool(out, allowBlankValue);
        ExtUtil.write(out, new ExtWrapNullable(exclude == null ? null : new ExtWrapTagged(exclude)));
        ExtUtil.write(out, new ExtWrapNullable(required == null ? null : new ExtWrapTagged(required)));
        ExtUtil.write(out, new ExtWrapNullable(requiredMsg));
        ExtUtil.write(out, new ExtWrapNullable(validation));
    }

    public String getKey() {
        return key;
    }

    @Nullable
    public String getAppearance() {
        return appearance;
    }

    @Nullable
    public String getInput() {
        return input;
    }

    @Nullable
    public String getReceive() {
        return receive;
    }

    @Nullable
    public String getHidden() {
        return hidden;
    }

    public boolean isAllowBlankValue() {
        return allowBlankValue;
    }

    public DisplayUnit getDisplay() {
        return display;
    }

    @Nullable
    public ItemsetBinding getItemsetBinding() {
        return itemsetBinding;
    }

    @Nullable
    public XPathExpression getDefaultValueExpr() {
        return defaultValueExpr;
    }

    public XPathExpression getExclude() {
        return exclude;
    }

    @Nullable
    public XPathExpression getRequired() {
        return required;
    }

    @Nullable
    public String getRequiredMsg() {
        return requiredMsg;
    }

    @Nullable
    public QueryPromptValidation getValidation() {
        return validation;
    }

    /**
     * @return whether the prompt has associated choices to select from
     */
    public boolean isSelect() {
        return getItemsetBinding() != null;
    }

}
