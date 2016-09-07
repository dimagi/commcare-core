package org.javarosa.core.model.data;

import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A response to a question requesting a selection of
 * any number of items from a list.
 *
 * @author Drew Roos
 */
public class SelectMultiData implements IAnswerData {
    private Vector<Selection> vs; //vector of Selection

    /**
     * Empty Constructor, necessary for dynamic construction during deserialization.
     * Shouldn't be used otherwise.
     */
    public SelectMultiData() {

    }

    public SelectMultiData(Vector<Selection> vs) {
        setValue(vs);
    }

    @Override
    public IAnswerData clone() {
        Vector<Selection> v = new Vector<>();
        for (int i = 0; i < vs.size(); i++) {
            v.addElement(vs.elementAt(i).clone());
        }
        return new SelectMultiData(v);
    }

    @Override
    public void setValue(Object o) {
        if (o == null) {
            throw new NullPointerException("Attempt to set an IAnswerData class to null.");
        }

        vs = new Vector<>((Vector<Selection>)o);
    }

    @Override
    public Vector<Selection> getValue() {
        return new Vector<>(vs);
    }

    @Override
    public String getDisplayText() {
        String str = "";

        for (int i = 0; i < vs.size(); i++) {
            Selection s = vs.elementAt(i);
            str += s.getValue();
            if (i < vs.size() - 1)
                str += ", ";
        }

        return str;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        vs = (Vector)ExtUtil.read(in, new ExtWrapList(Selection.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapList(vs));
    }

    @Override
    public UncastData uncast() {
        Enumeration en = vs.elements();
        StringBuffer selectString = new StringBuffer();

        while (en.hasMoreElements()) {
            Selection selection = (Selection)en.nextElement();
            if (selectString.length() > 0)
                selectString.append(" ");
            selectString.append(selection.getValue());
        }
        //As Crazy, and stupid, as it sounds, this is the XForms specification
        //for storing multiple selections.
        return new UncastData(selectString.toString());
    }

    @Override
    public SelectMultiData cast(UncastData data) throws IllegalArgumentException {
        Vector<Selection> v = new Vector<>();
        String[] choices = DataUtil.splitOnSpaces(data.value);
        for (String s : choices) {
            v.addElement(new Selection(s));
        }
        return new SelectMultiData(v);
    }

    public boolean isInSelection(String value) {
        for(Selection s : vs) {
            if (s.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
