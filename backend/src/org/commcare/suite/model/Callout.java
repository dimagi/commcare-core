package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by wpride1 on 4/14/15.
 */
public class Callout implements Externalizable, DetailTemplate {

    private String actionName;
    private String image;
    private final String displayName;
    private final Hashtable<String, String> extras = new Hashtable<String, String>();
    private final Vector<String> responses = new Vector<String>();

    public Callout(String actionName, String image, String displayName) {
        this.actionName = actionName;
        this.image = image;
        this.displayName = displayName;
    }

    @Override
    public CalloutData evaluate(EvaluationContext context) {
        Hashtable<String, String> evaluatedExtras = new Hashtable<String, String>();

        // evaluate extra xpaths down to values
        for (String key : extras.keySet()) {
            try {
                String value =
                        XPathFuncExpr.toString(XPathParseTool.parseXPath(extras.get(key)).eval(context));
                evaluatedExtras.put(key, value);
            } catch (XPathSyntaxException e) {
                // ignore extra xpaths that have evaluation errors
            }
        }

        // emit a CalloutData with the extras evaluated. used for the detail screen.
        return new CalloutData(actionName, image, displayName, evaluatedExtras, responses);
    }

    public CalloutData evaluate() {
        // emit a callout without the extras evaluated. used for the case list button.
        return new CalloutData(actionName, image, displayName, extras, responses);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException,
            DeserializationException {
        actionName = (String)ExtUtil.read(in, new ExtWrapNullable(String.class));
        image = (String)ExtUtil.read(in, new ExtWrapNullable(String.class));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapNullable(actionName));
        ExtUtil.write(out, new ExtWrapNullable(image));
    }

    public String getImage() {
        return image;
    }

    public String getActionName() {
        return actionName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void addExtra(String key, String value) {
        extras.put(key, value);
    }

    public void addResponse(String key) {
        responses.add(key);
    }

    public Hashtable<String, String> getExtras() {
        return extras;
    }

    public Vector<String> getResponses() {
        return responses;
    }
}