package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

// Model for <prompt> node in {@link
public class QueryPrompt implements Externalizable {

    private String key;

    @Nullable
    private String appearance;

    @Nullable
    private String input;

    private DisplayUnit display;

    @SuppressWarnings("unused")
    public QueryPrompt() {
    }

    public QueryPrompt(String key, String appearance, String input, DisplayUnit display) {
        this.key = key;
        this.appearance = appearance;
        this.input = input;
        this.display = display;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        key = (String)ExtUtil.read(in, String.class, pf);
        appearance = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        input = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        display = (DisplayUnit)ExtUtil.read(in, DisplayUnit.class, pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, key);
        ExtUtil.write(out, new ExtWrapNullable(appearance));
        ExtUtil.write(out, new ExtWrapNullable(input));
        ExtUtil.write(out, display);
    }

    public String getKey() {
        return key;
    }

    public String getAppearance() {
        return appearance;
    }

    public String getInput() {
        return input;
    }

    public DisplayUnit getDisplay() {
        return display;
    }
}
