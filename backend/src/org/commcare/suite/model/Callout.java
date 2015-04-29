package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by wpride1 on 4/14/15.
 *
 * Object representation of application callouts described in suite.xml
 * Used in callouts from EntitySelectActivity and EntityDetailActivity
 *
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
    public Callout evaluate(EvaluationContext context) {
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        displayName = ExtUtil.readString(in);
        actionName = (String)ExtUtil.read(in, new ExtWrapNullable(String.class));
        image = (String)ExtUtil.read(in, new ExtWrapNullable(String.class));
        extras = (Hashtable<String, String>) ExtUtil.read(in, new ExtWrapMap(String.class, String.class));
        responses = (Vector<String>)ExtUtil.read(in, new ExtWrapList(String.class), pf);
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, displayName);
        ExtUtil.write(out, new ExtWrapNullable(actionName));
        ExtUtil.write(out, new ExtWrapNullable(image));
        ExtUtil.write(out, new ExtWrapMap(extras));
        ExtUtil.write(out, new ExtWrapList(responses));
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
