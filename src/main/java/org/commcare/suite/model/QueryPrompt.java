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

    public static final CharSequence INPUT_TYPE_SELECT1 = "select1";

    private String key;

    @Nullable
    private String appearance;

    @Nullable
    private String input;

    private DisplayUnit display;

    @Nullable
    private XPathExpression defaultValueExpr;

    @Nullable
    private ItemsetBinding itemsetBinding;

    @SuppressWarnings("unused")
    public QueryPrompt() {
    }

    public QueryPrompt(String key, String appearance, String input, DisplayUnit display, ItemsetBinding itemsetBinding, XPathExpression defaultValueExpr) {
        this.key = key;
        this.appearance = appearance;
        this.input = input;
        this.display = display;
        this.itemsetBinding = itemsetBinding;
        this.defaultValueExpr = defaultValueExpr;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        key = (String)ExtUtil.read(in, String.class, pf);
        appearance = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        input = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        display = (DisplayUnit)ExtUtil.read(in, DisplayUnit.class, pf);
        itemsetBinding = (ItemsetBinding)ExtUtil.read(in, new ExtWrapNullable(ItemsetBinding.class), pf);
        defaultValueExpr = (XPathExpression)ExtUtil.read(in, new ExtWrapNullable(new ExtWrapTagged()), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, key);
        ExtUtil.write(out, new ExtWrapNullable(appearance));
        ExtUtil.write(out, new ExtWrapNullable(input));
        ExtUtil.write(out, display);
        ExtUtil.write(out, new ExtWrapNullable(itemsetBinding));
        ExtUtil.write(out, new ExtWrapNullable(defaultValueExpr == null ? null : new ExtWrapTagged(defaultValueExpr)));
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
}
