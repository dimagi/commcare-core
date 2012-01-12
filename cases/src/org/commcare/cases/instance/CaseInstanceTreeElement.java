/**
 * 
 */
package org.commcare.cases.instance;

import java.util.Vector;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

/**
 * The root element for the <casedb> abstract type. All children are
 * nodes in the case database. Depending on instantiation, the <casedb>
 * may include only a subset of the full db. 
 * 
 * @author ctsims
 *
 */
public class CaseInstanceTreeElement implements AbstractTreeElement<CaseChildElement> {

	private AbstractTreeElement instanceRoot;
	
	private IStorageUtilityIndexed storage;
	private String[] caseRecords;
	
	private Vector<CaseChildElement> cases;
	
	TreeElementCache cache = new TreeElementCache(300);
	
	public CaseInstanceTreeElement(AbstractTreeElement instanceRoot, IStorageUtilityIndexed storage, String[] caseIDs) {
		this(instanceRoot, storage);
		this.caseRecords = caseIDs;
	}
	
	public CaseInstanceTreeElement(AbstractTreeElement instanceRoot, IStorageUtilityIndexed storage) {
		this.instanceRoot= instanceRoot;
		this.storage = storage;
		storage.setReadOnly();
	}
	
	public void rebase(AbstractTreeElement instanceRoot) {
		this.instanceRoot = instanceRoot;
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#isLeaf()
	 */
	public boolean isLeaf() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#isChildable()
	 */
	public boolean isChildable() {
		// TODO Auto-generated method stub
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
	public CaseChildElement getChild(String name, int multiplicity) {
		if(multiplicity == TreeReference.INDEX_TEMPLATE) {
			return null;
		}
		
		//name is always "case", so multiplicities are the only relevant component here
		if(name.equals("case")) { 
			getCases();
			if(cases.size() == 0) {
				//If we have no cases, we still need to be able to return a template element so as to not
				//break xpath evaluation
				return CaseChildElement.TemplateElement(this);
			}
			return cases.elementAt(multiplicity);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildrenWithName(java.lang.String)
	 */
	public Vector getChildrenWithName(String name) {
		if(name.equals("case")) {
			getCases();
			return cases;
		} else {
			return new Vector();
		}
		
	}
	
	int numRecords = -1;

	public boolean hasChildren() {
		if(getNumChildren() > 0) {
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getNumChildren()
	 */
	public int getNumChildren() {
		if(caseRecords != null) {
			return caseRecords.length;
		} else {
			if(numRecords == -1) {
				numRecords = storage.getNumRecords();
			}
			return numRecords;
		}
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildAt(int)
	 */
	public CaseChildElement getChildAt(int i) {
		getCases();
		return cases.elementAt(i);
	}
	
	private void getCases() {
		if(cases != null) {
			return;
		}
		cases = new Vector<CaseChildElement>();
		if(caseRecords != null) {
			int i = 0;
			for(String id : caseRecords) {
				cases.addElement(new CaseChildElement(this, -1, id, i, storage, cache));
				++i;
			}
		} else {
			int mult = 0;
			for(IStorageIterator i = storage.iterate(); i.hasMore();) {
				int id = i.nextID();
				cases.addElement(new CaseChildElement(this, id, null, mult, storage, cache));
				mult++;
			}
			
		}
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#isRepeatable()
	 */
	public boolean isRepeatable() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#isAttribute()
	 */
	public boolean isAttribute() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildMultiplicity(java.lang.String)
	 */
	public int getChildMultiplicity(String name) {
		//All children have the same name;
		if(name.equals("case")) {
			return this.getNumChildren();
		} else {
			return 0;
		}
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
	public CaseChildElement getAttribute(String namespace, String name) {
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
		return "casedb";
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
		return instanceRoot;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getValue()
	 */
	public IAnswerData getValue() {
		// TODO Auto-generated method stub
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

}
