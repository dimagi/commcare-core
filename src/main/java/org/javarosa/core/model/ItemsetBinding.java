package org.javarosa.core.model;

import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

public class ItemsetBinding implements Externalizable {

    /**
     * note that storing both the ref and expr for everything is kind of redundant, but we're forced
     * to since it's nearly impossible to convert between the two w/o having access to the underlying
     * xform/xpath classes, which we don't from the core model project
     */

    public TreeReference nodesetRef;   //absolute ref of itemset source nodes
    public IConditionExpr nodesetExpr; //path expression for source nodes; may be relative, may contain predicates
    public TreeReference contextRef;   //context ref for nodesetExpr; ref of the control parent (group/formdef) of itemset question
    //note: this is only here because its currently impossible to both (a) get a form control's parent, and (b)
    //convert expressions into refs while preserving predicates. once these are fixed, this field can go away

    public TreeReference labelRef;     //absolute ref of label
    public IConditionExpr labelExpr;   //path expression for label; may be relative, no predicates
    public boolean labelIsItext;       //if true, content of 'label' is an itext id

    public boolean copyMode;           //true = copy subtree; false = copy string value
    public TreeReference copyRef;      //absolute ref to copy

    public TreeReference valueRef;     //absolute ref to value
    public IConditionExpr valueExpr;   //path expression for value; may be relative, no predicates (must be relative if copy mode)

    public TreeReference sortRef;     //absolute ref to sort
    public IConditionExpr sortExpr;   //path expression for sort; may be relative, no predicates (must be relative if copy mode)

    private TreeReference destRef; //ref that identifies the repeated nodes resulting from this itemset

    // dynamic choices, not serialized
    private Vector<SelectChoice> choices;

    public Vector<SelectChoice> getChoices() {
        return choices;
    }

    public void setChoices(Vector<SelectChoice> choices) {
        if (this.choices != null) {
            System.out.println("warning: previous choices not cleared out");
            clearChoices();
        }
        this.choices = choices;
        sortChoices();
    }

    private void sortChoices() {
        if (this.sortRef != null) {

            // Perform sort
            Collections.sort(choices, new Comparator<SelectChoice>() {
                @Override
                public int compare(SelectChoice choice1, SelectChoice choice2) {
                    return choice1.evaluatedSortProperty.compareTo(choice2.evaluatedSortProperty);
                }
            });

            // Re-set indices after sorting
            for (int i = 0; i < choices.size(); i++) {
                choices.get(i).setIndex(i);
            }
        }
    }

    public void clearChoices() {
        this.choices = null;
    }

    public void setDestRef(QuestionDef q) {
        destRef = FormInstance.unpackReference(q.getBind()).clone();
        if (copyMode) {
            destRef.add(copyRef.getNameLast(), TreeReference.INDEX_UNBOUND);
        }
    }

    public TreeReference getDestRef() {
        return destRef;
    }

    public IConditionExpr getRelativeValue() {
        TreeReference relRef = null;

        if (copyRef == null) {
            relRef = valueRef; //must be absolute in this case
        } else if (valueRef != null) {
            relRef = valueRef.relativize(copyRef);
        }

        return relRef != null ? RestoreUtils.refToPathExpr(relRef) : null;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        nodesetRef = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
        nodesetExpr = (IConditionExpr)ExtUtil.read(in, new ExtWrapTagged(), pf);
        contextRef = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
        labelRef = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
        labelExpr = (IConditionExpr)ExtUtil.read(in, new ExtWrapTagged(), pf);
        valueRef = (TreeReference)ExtUtil.read(in, new ExtWrapNullable(TreeReference.class), pf);
        valueExpr = (IConditionExpr)ExtUtil.read(in, new ExtWrapNullable(new ExtWrapTagged()), pf);
        copyRef = (TreeReference)ExtUtil.read(in, new ExtWrapNullable(TreeReference.class), pf);
        labelIsItext = ExtUtil.readBool(in);
        copyMode = ExtUtil.readBool(in);
        sortRef = (TreeReference)ExtUtil.read(in, new ExtWrapNullable(TreeReference.class), pf);
        sortExpr = (IConditionExpr)ExtUtil.read(in, new ExtWrapNullable(new ExtWrapTagged()), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, nodesetRef);
        ExtUtil.write(out, new ExtWrapTagged(nodesetExpr));
        ExtUtil.write(out, contextRef);
        ExtUtil.write(out, labelRef);
        ExtUtil.write(out, new ExtWrapTagged(labelExpr));
        ExtUtil.write(out, new ExtWrapNullable(valueRef));
        ExtUtil.write(out, new ExtWrapNullable(valueExpr == null ? null : new ExtWrapTagged(valueExpr)));
        ExtUtil.write(out, new ExtWrapNullable(copyRef));
        ExtUtil.writeBool(out, labelIsItext);
        ExtUtil.writeBool(out, copyMode);
        ExtUtil.write(out, new ExtWrapNullable(sortRef));
        ExtUtil.write(out, new ExtWrapNullable(sortExpr == null ? null : new ExtWrapTagged(sortExpr)));
    }
}
