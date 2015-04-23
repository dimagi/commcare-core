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
public class Callout implements Externalizable, DetailTemplate{

    String actionName;
    String image;
    String displayName;
    Hashtable<String, String> extras = new Hashtable<String, String>();
    Vector<String> responses = new Vector<String>();

    public Callout(String actionName, String image, String displayName){
        this.actionName = actionName;
        this.image = image;
        this.displayName = displayName;
    }

    /*
    * (non-Javadoc)
    * @see org.commcare.suite.model.DetailTemplate#evaluate(org.javarosa.core.model.condition.EvaluationContext)
    */
    public CalloutData evaluate(EvaluationContext context) {

        Hashtable<String, String> evaluatedExtras = new Hashtable<String, String>();

        for(String key : extras.keySet()){
            try {
                String evaluatedKey = XPathFuncExpr.toString(XPathParseTool.parseXPath(extras.get(key)).eval(context));
                evaluatedExtras.put(key, evaluatedKey);
            } catch(XPathSyntaxException e){
                // do nothing
            }
        }

        // emit a CalloutData with the extras evaluated. used for the detail screen.
        CalloutData ret = new CalloutData(actionName, image, displayName, evaluatedExtras, responses);

        return ret;
    }

    public CalloutData evaluate() {

        //emit a callout without the extras evaluated. used for the case list button.
        CalloutData ret = new CalloutData(actionName, image, displayName, extras, responses);

        return ret;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        actionName = (String)ExtUtil.read(in, new ExtWrapNullable(String.class));
        image = (String)ExtUtil.read(in, new ExtWrapNullable(String.class));
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapNullable(actionName));
        ExtUtil.write(out, new ExtWrapNullable(image));
    }

    public String getImage(){
        return image;
    }

    public String getActionName(){
        return actionName;
    }

    public String getDisplayName() { return displayName;}

    public void addExtra(String key, String value) {
        extras.put(key, value);
    }

    public void addResponse(String key){
        responses.add(key);
    }

    public Hashtable<String, String> getExtras(){
        return extras;
    }

    public Vector<String> getResponses(){
        return responses;
    }
}
