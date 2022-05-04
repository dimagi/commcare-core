package org.commcare.suite.model;

import org.commcare.modern.util.Pair;
import org.javarosa.core.model.ItemsetBinding;
import org.javarosa.core.model.SelectChoice;
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
import java.util.Vector;

import javax.annotation.Nullable;

// Model for <prompt> node in {@link
public class QueryPrompt implements Externalizable {

    public static final String INPUT_TYPE_SELECT1 = "select1";
    public static final String INPUT_TYPE_SELECT = "select";
    public static final String INPUT_TYPE_DATERANGE = "daterange";
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

    @SuppressWarnings("unused")
    public QueryPrompt() {
    }

    public QueryPrompt(String key, String appearance, String input, String receive,
                       String hidden, DisplayUnit display, ItemsetBinding itemsetBinding, 
                       XPathExpression defaultValueExpr, XPathExpression exclude) {
        this.key = key;
        this.appearance = appearance;
        this.input = input;
        this.receive = receive;
        this.hidden = hidden;
        this.display = display;
        this.itemsetBinding = itemsetBinding;
        this.defaultValueExpr = defaultValueExpr;
        this.exclude = exclude;
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
        exclude = (XPathExpression)ExtUtil.read(in, new ExtWrapNullable(new ExtWrapTagged()), pf);
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
        ExtUtil.write(out, new ExtWrapNullable(exclude == null ? null : new ExtWrapTagged(exclude)));
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

    /**
     * @return whether the prompt has associated choices to select from
     */
    public boolean isSelect() {
        return getItemsetBinding() != null;
    }

    public XPathExpression getExclude() {
        return exclude;
    }

}
