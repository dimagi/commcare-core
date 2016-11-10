package org.javarosa.core.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by wpride1 on 5/7/15.
 *
 * Represents the compelte set of text strings that can be displayed
 * with a question. does not contain multimedia references.
 */
public class QuestionString implements Externalizable {

    private String name;
    private String textId;
    private String textInner;
    private String textFallback;

    public QuestionString(){
        
    }
    
    public QuestionString(String name){
        this.name = name;
    }

    public QuestionString(String name, String inner){
        this.name = name;
        this.textInner = inner;
    }

    public String getName(){
        return name;
    }

    public void setTextId(String textId){
        this.textId = textId;
    }

    public String getTextId(){
        return textId;
    }

    public void setTextFallback(String textFallback){
        this.textFallback = textFallback;
    }

    public String getTextFallback(){
        return textFallback;
    }

    public void setTextInner(String textInner){
        this.textInner = textInner;
    }

    public String getTextInner(){
        return textInner;
    }

    public String toString(){
        return "Name: " + name + " ID: " + textId + " Inner: "
                + textInner + " Fallback: " + textFallback;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        name = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        textId = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        textInner = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        textFallback = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapNullable(name));
        ExtUtil.write(out, new ExtWrapNullable(textId));
        ExtUtil.write(out, new ExtWrapNullable(textInner));
        ExtUtil.write(out, new ExtWrapNullable(textFallback));
    }
}
