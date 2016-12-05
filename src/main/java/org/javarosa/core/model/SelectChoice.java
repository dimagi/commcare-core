package org.javarosa.core.model;

import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xform.parse.XFormParseException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SelectChoice implements Externalizable {

    private String labelInnerText;
    private String textID;
    private boolean isLocalizable;
    private String value;
    private int index = -1;

    public TreeElement copyNode; //if this choice represents part of an <itemset>, and the itemset uses 'copy'
    //answer mode, this points to the node to be copied if this selection is chosen
    //this field only has meaning for dynamic choices, thus is unserialized

    //for deserialization only
    public SelectChoice() {

    }

    public SelectChoice(String labelID, String value) {
        this(labelID, null, value, true);
    }

    /**
     * @param labelID        can be null
     * @param labelInnerText can be null
     * @param value          should not be null
     * @throws XFormParseException if value is null
     */
    public SelectChoice(String labelID, String labelInnerText, String value, boolean isLocalizable) {
        this.isLocalizable = isLocalizable;
        this.textID = labelID;
        this.labelInnerText = labelInnerText;
        if (value != null) {
            this.value = value;
        } else {
            throw new XFormParseException("SelectChoice{id,innerText}:{" + labelID + "," + labelInnerText + "}, has null Value!");
        }
    }

    public SelectChoice(String labelOrID, String Value, boolean isLocalizable) {
        this(isLocalizable ? labelOrID : null,
                isLocalizable ? null : labelOrID,
                Value, isLocalizable);
    }

    public void setIndex(int index) {
        this.index = index;
    }


    public String getLabelInnerText() {
        return labelInnerText;
    }

    public String getValue() {
        return value;
    }

    public int getIndex() {
        if (index == -1) {
            throw new RuntimeException("trying to access choice index before it has been set!");
        }

        return index;
    }

    public boolean isLocalizable() {
        return this.isLocalizable;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        isLocalizable = ExtUtil.readBool(in);
        setLabelInnerText(ExtUtil.nullIfEmpty(ExtUtil.readString(in)));
        setTextID(ExtUtil.nullIfEmpty(ExtUtil.readString(in)));
        value = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        //index will be set by questiondef
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeBool(out, isLocalizable);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(labelInnerText));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(textID));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(value));
        //don't serialize index; it will be restored from questiondef
    }

    private void setLabelInnerText(String labelInnerText) {
        this.labelInnerText = labelInnerText;
    }

    public Selection selection() {
        return new Selection(this);
    }

    public String toString() {
        return ((textID != null && textID != "") ? "{" + textID + "}" : "") + (labelInnerText != null ? labelInnerText : "") + " => " + value;
    }

    public String getTextID() {
        return textID;
    }

    public void setTextID(String textID) {
        this.textID = textID;
    }

    @Override
    public int hashCode() {
        int result;
        result = textID != null ? textID.hashCode() : 0;
        result = result ^ value.hashCode();
        result = result ^ index;
        result = result ^ (isLocalizable ? 1 : 0);
        result = result ^ (labelInnerText != null ? labelInnerText.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof SelectChoice)) {
            return false;
        }

        SelectChoice otherChoice = (SelectChoice)obj;

        String otherTextID = otherChoice.getTextID();
        if (otherTextID == null) {
            if (this.textID != null) {
                return false;
            }
        }
        else if (!otherTextID.equals(this.textID)) {
            return false;
        }

        if (!otherChoice.getValue().equals(this.value)) {
            return false;
        }

        if (otherChoice.getIndex() != this.index) {
            return false;
        }

        if (otherChoice.isLocalizable() != this.isLocalizable) {
            return false;
        }

        String otherLabelText = otherChoice.getLabelInnerText();
        if (otherLabelText == null) {
            if (this.labelInnerText != null) {
                return false;
            }
        }
        else if (!otherLabelText.equals(this.labelInnerText)) {
            return false;
        }

        return true;
    }
}
