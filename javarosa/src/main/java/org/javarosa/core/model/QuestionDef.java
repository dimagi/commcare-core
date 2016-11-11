package org.javarosa.core.model;

import org.javarosa.core.model.actions.ActionController;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xform.parse.XFormParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The definition of a Question to be presented to users when
 * filling out a form.
 *
 * QuestionDef requires that any XPathReferences that are used
 * are contained in the FormDefRMS's PrototypeFactoryDeprecated in order
 * to be properly deserialized. If they aren't, an exception
 * will be thrown at the time of deserialization.
 *
 * @author Daniel Kayiwa/Drew Roos
 */
public class QuestionDef implements IFormElement {
    private int id;

    // reference to the location in the model from which to load data for the question,
    // and store data back to when question is answered
    private XPathReference binding;

    // The type of widget. eg TextInput,Slider,List etc.
    private int controlType;
    private String appearanceAttr;

    private Vector<SelectChoice> choices;
    private ItemsetBinding dynamicChoices;

    private Hashtable<String, QuestionString> mQuestionStrings;

    Vector<QuestionDataExtension> extensions;

    private ActionController actionController;

    public QuestionDef() {
        this(Constants.NULL_ID, Constants.DATATYPE_TEXT);
    }

    public QuestionDef(int id, int controlType) {
        setID(id);
        setControlType(controlType);
        mQuestionStrings = new Hashtable<>();
        extensions = new Vector<>();
        
        //ctsims 7/8/2015 - Some of Will's code seems to assume that there's ~always a label 
        //defined, which is causing problems with blank questions. Adding this for now to ensure things
        //work reliably
        mQuestionStrings.put(XFormParser.LABEL_ELEMENT, new QuestionString(XFormParser.LABEL_ELEMENT, null));
        actionController = new ActionController();
    }

    public void putQuestionString(String key, QuestionString value){
        mQuestionStrings.put(key, value);
    }

    public QuestionString getQuestionString(String key){
        return mQuestionStrings.get(key);
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

    public int getControlType() {
        return controlType;
    }

    public void setControlType(int controlType) {
        this.controlType = controlType;
    }

    @Override
    public String getAppearanceAttr() {
        return appearanceAttr;
    }

    @Override
    public void setAppearanceAttr(String appearanceAttr) {
        this.appearanceAttr = appearanceAttr;
    }

    @Override
    public ActionController getActionController() {
        return this.actionController;
    }

    public String getHelpTextID() {
        return mQuestionStrings.get(XFormParser.HELP_ELEMENT) == null ? null : mQuestionStrings.get(XFormParser.HELP_ELEMENT).getTextId();
    }

    public void addSelectChoice(SelectChoice choice) {
        if (choices == null) {
            choices = new Vector<>();
        }
        choice.setIndex(choices.size());
        choices.addElement(choice);
    }

    public void removeSelectChoice(SelectChoice choice) {
        if (choices == null) {
            choice.setIndex(0);
            return;
        }

        if (choices.contains(choice)) {
            choices.removeElement(choice);
        }
    }

    public Vector<SelectChoice> getChoices() {
        return choices;
    }

    public SelectChoice getChoice(int i) {
        return choices.elementAt(i);
    }

    public int getNumChoices() {
        return (choices != null ? choices.size() : 0);
    }

    public SelectChoice getChoiceForValue(String value) {
        for (int i = 0; i < getNumChoices(); i++) {
            if (getChoice(i).getValue().equals(value)) {
                return getChoice(i);
            }
        }
        return null;
    }

    public ItemsetBinding getDynamicChoices() {
        return dynamicChoices;
    }

    public void setDynamicChoices(ItemsetBinding ib) {
        if (ib != null) {
            ib.setDestRef(this);
        }
        this.dynamicChoices = ib;
    }

    /**
     * Determine if a question's answer is xml tree data.
     *
     * @return does the answer to this question yields xml tree data, and not a simple string value?
     */
    public boolean isComplex() {
        return (dynamicChoices != null && dynamicChoices.copyMode);
    }

    @Override
    public Vector<IFormElement> getChildren() {
        return null;
    }

    @Override
    public void setChildren(Vector v) {
        throw new IllegalStateException("Can't add children to question def");
    }

    @Override
    public void addChild(IFormElement fe) {
        throw new IllegalStateException("Can't add children to question def");
    }

    @Override
    public IFormElement getChild(int i) {
        return null;
    }

    @Override
    public void readExternal(DataInputStream dis, PrototypeFactory pf) throws IOException, DeserializationException {
        setID(ExtUtil.readInt(dis));
        binding = (XPathReference)ExtUtil.read(dis, new ExtWrapNullable(new ExtWrapTagged()), pf);
        setAppearanceAttr((String) ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setControlType(ExtUtil.readInt(dis));
        choices = ExtUtil.nullIfEmpty((Vector)ExtUtil.read(dis, new ExtWrapList(SelectChoice.class), pf));
        for (int i = 0; i < getNumChoices(); i++) {
            choices.elementAt(i).setIndex(i);
        }
        setDynamicChoices((ItemsetBinding)ExtUtil.read(dis, new ExtWrapNullable(ItemsetBinding.class), pf));
        mQuestionStrings = (Hashtable<String, QuestionString>)ExtUtil.read(dis, new ExtWrapMap(String.class, QuestionString.class), pf);
        extensions = (Vector)ExtUtil.read(dis, new ExtWrapListPoly(), pf);
        actionController = (ActionController)ExtUtil.read(dis, new ExtWrapNullable(ActionController.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream dos) throws IOException {
        ExtUtil.writeNumeric(dos, getID());
        ExtUtil.write(dos, new ExtWrapNullable(binding == null ? null : new ExtWrapTagged(binding)));
        ExtUtil.write(dos, new ExtWrapNullable(getAppearanceAttr()));
        ExtUtil.writeNumeric(dos, getControlType());
        ExtUtil.write(dos, new ExtWrapList(ExtUtil.emptyIfNull(choices)));
        ExtUtil.write(dos, new ExtWrapNullable(dynamicChoices));
        ExtUtil.write(dos, new ExtWrapMap(mQuestionStrings));
        ExtUtil.write(dos, new ExtWrapListPoly(extensions));
        ExtUtil.write(dos, new ExtWrapNullable(actionController));
    }

    @Override
    public int getDeepChildCount() {
        return 1;
    }

    @Override
    public String getTextID() {
        return this.getQuestionString(XFormParser.LABEL_ELEMENT).getTextId();
    }

    @Override
    public String getLabelInnerText() {
        return this.getQuestionString(XFormParser.LABEL_ELEMENT).getTextInner();
    }

    @Override
    public void setTextID(String textID) {
        if (DateUtils.stringContains(textID, ";")) {
            System.err.println("Warning: TextID contains ;form modifier:: \"" + textID.substring(textID.indexOf(";")) + "\"... will be stripped.");
            textID = textID.substring(0, textID.indexOf(";")); //trim away the form specifier
        }
        this.getQuestionString(XFormParser.LABEL_ELEMENT).setTextId(textID);
    }

    public void addExtension(QuestionDataExtension extension) {
        extensions.addElement(extension);
    }

    public Vector<QuestionDataExtension> getExtensions() {
        return this.extensions;
    }
}
