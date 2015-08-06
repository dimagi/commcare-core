/**
 *
 */
package org.javarosa.core.model.instance;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.reference.Reference;
import org.javarosa.xpath.expr.XPathExpression;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

/**
 * The root element for the <casedb> abstract type. All children are
 * nodes in the case database. Depending on instantiation, the <casedb>
 * may include only a subset of the full db.
 *
 * @author ctsims
 */
public class XmlWalkingTreeElement implements AbstractTreeElement<XmlWalkingTreeElement> {

    private AbstractTreeElement instanceRoot;
    private KXmlParser parser;
    InputStreamReader reader;

    public XmlWalkingTreeElement(AbstractTreeElement instanceRoot, Reference ref) {
        this.instanceRoot = instanceRoot;

        parser = new KXmlParser();
        try {
            InputStream stream = ref.getStream();
            reader = new InputStreamReader(stream);
            parser.setInput(reader);


        } catch (XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isLeaf()
     */
    public boolean isLeaf() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isChildable()
     */
    public boolean isChildable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasChildren() {
        if (getNumChildren() > 0) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getInstanceName()
     */
    public String getInstanceName() {
        return instanceRoot.getInstanceName();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChild(java.lang.String, int)
     */
    public XmlWalkingTreeElement getChild(String name, int multiplicity) {
        if (multiplicity == TreeReference.INDEX_TEMPLATE) {
            return null;
        }
        //Implement
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildrenWithName(java.lang.String)
     */
    public Vector getChildrenWithName(String name) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getNumChildren()
     */
    public int getNumChildren() {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildAt(int)
     */
    public XmlWalkingTreeElement getChildAt(int i) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isRepeatable()
     */
    public boolean isRepeatable() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isAttribute()
     */
    public boolean isAttribute() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildMultiplicity(java.lang.String)
     */
    public int getChildMultiplicity(String name) {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isRelevant()
     */
    public boolean isRelevant() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#accept(org.javarosa.core.model.instance.utils.ITreeVisitor)
     */
    public void accept(ITreeVisitor visitor) {
        visitor.visit(this);

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeCount()
     */
    public int getAttributeCount() {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeNamespace(int)
     */
    public String getAttributeNamespace(int index) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeName(int)
     */
    public String getAttributeName(int index) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(int)
     */
    public String getAttributeValue(int index) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttribute(java.lang.String, java.lang.String)
     */
    public XmlWalkingTreeElement getAttribute(String namespace, String name) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(java.lang.String, java.lang.String)
     */
    public String getAttributeValue(String namespace, String name) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getRef()
     */
    public TreeReference getRef() {
        return TreeElement.BuildRef(this);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getDepth()
     */
    public int getDepth() {
        return TreeElement.CalculateDepth(this);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getName()
     */
    public String getName() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getMult()
     */
    public int getMult() {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getParent()
     */
    public AbstractTreeElement getParent() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getValue()
     */
    public IAnswerData getValue() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getDataType()
     */
    public int getDataType() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void clearCaches() {
        // TODO Auto-generated method stub

    }

    public Vector<TreeReference> tryBatchChildFetch(String name, int mult,
                                                    Vector<XPathExpression> predicates, EvaluationContext evalContext) {
        return null;
    }

    public String getNamespace() {
        // TODO Auto-generated method stub
        return null;
    }

}
