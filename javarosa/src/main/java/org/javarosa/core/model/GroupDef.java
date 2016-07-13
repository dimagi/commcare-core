package org.javarosa.core.model;

import org.javarosa.core.model.actions.ActionController;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.locale.Localizable;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * The definition of a group in a form or questionaire.
 *
 * @author Daniel Kayiwa
 */
public class GroupDef implements IFormElement, Localizable {
    // A list of questions on a group.
    private Vector<IFormElement> children;
    // True if this is a "repeat", false if it is a "group"
    private boolean repeat;
    // The group number.
    private int id;
    // reference to a location in the model to store data in
    private XPathReference binding;

    private String labelInnerText;
    private String appearanceAttr;
    private String textID;

    //custom phrasings for repeats
    public String chooseCaption;
    public String addCaption;
    public String delCaption;
    public String doneCaption;
    public String addEmptyCaption;
    public String doneEmptyCaption;
    public String entryHeader;
    public String delHeader;
    public String mainHeader;

    private final Vector<FormElementStateListener> observers;

    /**
     * When set the user can only create as many children as specified by the
     * 'count' reference.
     */
    public boolean noAddRemove = false;

    /**
     * Points to a reference that stores the expected number of entries for the
     * group.
     */
    public XPathReference count = null;

    public GroupDef() {
        this(Constants.NULL_ID, null, false);
    }

    public GroupDef(int id, Vector children, boolean repeat) {
        setID(id);
        setChildren(children);
        setRepeat(repeat);
        observers = new Vector<>();
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public void setID(int id) {
        this.id = id;
    }

    @Override
    public XPathReference getBind() {
        return binding;
    }

    public void setBind(XPathReference binding) {
        this.binding = binding;
    }

    @Override
    public Vector<IFormElement> getChildren() {
        return children;
    }

    @Override
    public void setChildren(Vector<IFormElement> children) {
        this.children = (children == null ? new Vector<IFormElement>() : children);
    }

    @Override
    public void addChild(IFormElement fe) {
        children.addElement(fe);
    }

    @Override
    public IFormElement getChild(int i) {
        if (children == null || i >= children.size()) {
            return null;
        } else {
            return children.elementAt(i);
        }
    }

    /**
     * @return true if this represents a <repeat> element
     */
    public boolean getRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public String getLabelInnerText() {
        return labelInnerText;
    }

    public void setLabelInnerText(String lit) {
        labelInnerText = lit;
    }

    @Override
    public String getAppearanceAttr() {
        return appearanceAttr;
    }

    public void setAppearanceAttr(String appearanceAttr) {
        this.appearanceAttr = appearanceAttr;
    }

    @Override
    public ActionController getActionController() {
        return null;
    }

    @Override
    public void localeChanged(String locale, Localizer localizer) {
        for (Enumeration e = children.elements(); e.hasMoreElements(); ) {
            ((IFormElement)e.nextElement()).localeChanged(locale, localizer);
        }
    }

    /**
     * @return Reference pointing to the number of entries this group should
     * have.
     */
    public XPathReference getCountReference() {
        return count;
    }

    /**
     * Contextualize the reference pointing to the repeat number limit in terms
     * of the inputted context. Used to contextualize the count reference in
     * terms of the current repeat item.
     *
     * @param context Used to resolve relative parts of the 'count' reference.
     *                Usually the current repeat item reference is passed in
     *                for this value.
     * @return An absolute reference that points to the numeric limit of repeat
     * items that should be created.
     */
    public TreeReference getConextualizedCountReference(TreeReference context) {
        return DataInstance.unpackReference(count).contextualize(context);
    }

    @Override
    public String toString() {
        return "<group>";
    }

    @Override
    public int getDeepChildCount() {
        int total = 0;
        Enumeration e = children.elements();
        while (e.hasMoreElements()) {
            total += ((IFormElement)e.nextElement()).getDeepChildCount();
        }
        return total;
    }

    /**
     * Reads a group definition object from the supplied stream.
     */
    @Override
    public void readExternal(DataInputStream dis, PrototypeFactory pf) throws IOException, DeserializationException {
        setID(ExtUtil.readInt(dis));
        setAppearanceAttr((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setBind((XPathReference)ExtUtil.read(dis, new ExtWrapTagged(), pf));
        setTextID((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setLabelInnerText((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setRepeat(ExtUtil.readBool(dis));
        setChildren((Vector)ExtUtil.read(dis, new ExtWrapListPoly(), pf));

        noAddRemove = ExtUtil.readBool(dis);
        count = (XPathReference)ExtUtil.read(dis, new ExtWrapNullable(new ExtWrapTagged()), pf);

        chooseCaption = ExtUtil.nullIfEmpty(ExtUtil.readString(dis));
        addCaption = ExtUtil.nullIfEmpty(ExtUtil.readString(dis));
        delCaption = ExtUtil.nullIfEmpty(ExtUtil.readString(dis));
        doneCaption = ExtUtil.nullIfEmpty(ExtUtil.readString(dis));
        addEmptyCaption = ExtUtil.nullIfEmpty(ExtUtil.readString(dis));
        doneEmptyCaption = ExtUtil.nullIfEmpty(ExtUtil.readString(dis));
        entryHeader = ExtUtil.nullIfEmpty(ExtUtil.readString(dis));
        delHeader = ExtUtil.nullIfEmpty(ExtUtil.readString(dis));
        mainHeader = ExtUtil.nullIfEmpty(ExtUtil.readString(dis));
    }

    /**
     * Write the group definition object to the supplied stream.
     */
    @Override
    public void writeExternal(DataOutputStream dos) throws IOException {
        ExtUtil.writeNumeric(dos, getID());
        ExtUtil.write(dos, new ExtWrapNullable(getAppearanceAttr()));
        ExtUtil.write(dos, new ExtWrapTagged(getBind()));
        ExtUtil.write(dos, new ExtWrapNullable(getTextID()));
        ExtUtil.write(dos, new ExtWrapNullable(getLabelInnerText()));
        ExtUtil.writeBool(dos, getRepeat());
        ExtUtil.write(dos, new ExtWrapListPoly(getChildren()));

        ExtUtil.writeBool(dos, noAddRemove);
        ExtUtil.write(dos, new ExtWrapNullable(count != null ? new ExtWrapTagged(count) : null));

        ExtUtil.writeString(dos, ExtUtil.emptyIfNull(chooseCaption));
        ExtUtil.writeString(dos, ExtUtil.emptyIfNull(addCaption));
        ExtUtil.writeString(dos, ExtUtil.emptyIfNull(delCaption));
        ExtUtil.writeString(dos, ExtUtil.emptyIfNull(doneCaption));
        ExtUtil.writeString(dos, ExtUtil.emptyIfNull(addEmptyCaption));
        ExtUtil.writeString(dos, ExtUtil.emptyIfNull(doneEmptyCaption));
        ExtUtil.writeString(dos, ExtUtil.emptyIfNull(entryHeader));
        ExtUtil.writeString(dos, ExtUtil.emptyIfNull(delHeader));
        ExtUtil.writeString(dos, ExtUtil.emptyIfNull(mainHeader));

    }

    @Override
    public void registerStateObserver(FormElementStateListener qsl) {
        if (!observers.contains(qsl)) {
            observers.addElement(qsl);
        }
    }

    @Override
    public void unregisterStateObserver(FormElementStateListener qsl) {
        observers.removeElement(qsl);
    }

    @Override
    public String getTextID() {
        return textID;
    }

    @Override
    public void setTextID(String textID) {
        if (textID == null) {
            this.textID = null;
            return;
        }
        if (DateUtils.stringContains(textID, ";")) {
            System.err.println("Warning: TextID contains ;form modifier:: \"" + textID.substring(textID.indexOf(";")) + "\"... will be stripped.");
            textID = textID.substring(0, textID.indexOf(";")); //trim away the form specifier
        }
        this.textID = textID;
    }
}
