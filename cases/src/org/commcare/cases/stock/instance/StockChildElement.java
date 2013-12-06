/**
 * 
 */
package org.commcare.cases.stock.instance;

import java.util.Enumeration;
import java.util.Vector;

import org.commcare.cases.model.CaseIndex;
import org.commcare.cases.stock.Stock;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.model.utils.PreloadUtils;
import org.javarosa.xpath.expr.XPathExpression;

/**
 * @author ctsims
 *
 */
public class StockChildElement implements AbstractTreeElement<TreeElement> {
	
	StockInstanceTreeElement parent;
	int recordId; 
	String entityId;
	int mult;
	
	TreeElement empty;
	
	int numChildren = -1;
	
	public StockChildElement(StockInstanceTreeElement parent, int recordId, String entityId, int mult) {
		if(recordId == -1 && entityId == null) { throw new RuntimeException("Cannot create a lazy case element with no lookup identifiers!");}
		this.parent = parent;
		this.recordId = recordId;
		this.entityId = entityId;
		this.mult = mult;
	}
	
	/*
	 * Template constructor (For elements that need to create reference nodesets but never look up values)
	 */
	private StockChildElement(StockInstanceTreeElement parent) {
		//Template
		this.parent = parent;
		this.recordId = TreeReference.INDEX_TEMPLATE;
		this.mult = TreeReference.INDEX_TEMPLATE;
		this.entityId = null;
		
		empty = new TreeElement();
		empty = new TreeElement("stock");
		empty.setMult(this.mult);
		
		empty.setAttribute(null, "id", "");
		
		TreeElement scratch = new TreeElement("product");
		scratch.setAttribute(null, "id", "");
		scratch.setAnswer(null);
		empty.addChild(scratch);
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

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getInstanceName()
	 */
	public String getInstanceName() {
		return parent.getInstanceName();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getChild(java.lang.String, int)
	 */
	public TreeElement getChild(String name, int multiplicity) {
		TreeElement cached = cache();
		TreeElement child = cached.getChild(name, multiplicity);
		if(multiplicity >= 0 && child == null) {
			TreeElement emptyNode = new TreeElement(name);
			cached.addChild(emptyNode);
			emptyNode.setParent(cached);
			return emptyNode;
		}
		return child;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildrenWithName(java.lang.String)
	 */
	public Vector getChildrenWithName(String name) {
		return cache().getChildrenWithName(name);
	}
	
	public boolean hasChildren() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getNumChildren()
	 */
	public int getNumChildren() {
		if(numChildren == -1) {
			numChildren = cache().getNumChildren();
		}
		return numChildren;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildAt(int)
	 */
	public TreeElement getChildAt(int i) {
		return cache().getChildAt(i);
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
		return cache().getChildMultiplicity(name);
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
		//TODO: Attributes should be fixed and possibly only include meta-details
		return cache().getAttributeCount();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeNamespace(int)
	 */
	public String getAttributeNamespace(int index) {
		return cache().getAttributeNamespace(index);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeName(int)
	 */
	public String getAttributeName(int index) {
		return cache().getAttributeName(index);

	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(int)
	 */
	public String getAttributeValue(int index) {
		return cache().getAttributeValue(index);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttribute(java.lang.String, java.lang.String)
	 */
	public TreeElement getAttribute(String namespace, String name) {
		if(name.equals("id")) {
			if(recordId != TreeReference.INDEX_TEMPLATE) {
				//if we're already cached, don't bother with this nonsense
				synchronized(parent.treeCache){
					TreeElement element = parent.treeCache.retrieve(recordId);
					if(element != null) {
						return cache().getAttribute(namespace, name);
					}
				}
			}
			
			//TODO: CACHE GET ID THING
			if(entityId == null) { return cache().getAttribute(namespace, name);}
			
			//otherwise, don't cache this just yet if we have the ID handy
			TreeElement caseid = TreeElement.constructAttributeElement(null, name);
			caseid.setValue(new StringData(entityId));
			caseid.setParent(this);
			return caseid;
		}
		return cache().getAttribute(namespace, name);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(java.lang.String, java.lang.String)
	 */
	public String getAttributeValue(String namespace, String name) {
		if(name.equals("id")) {
			return entityId;
		}
		return cache().getAttributeValue(namespace, name);
	}

	TreeReference ref;
	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getRef()
	 */
	public TreeReference getRef() {
		if(ref == null) {
			ref = TreeElement.BuildRef(this);
		}
		return ref;
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
		return "stock";
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getMult()
	 */
	public int getMult() {
		// TODO Auto-generated method stub
		return mult;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getParent()
	 */
	public AbstractTreeElement getParent() {
		return parent;
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
	
	//TODO: Thread Safety!
	public void clearCaches() {
		//cached = null;
	}
	
	//TODO: THIS IS NOT THREAD SAFE
	private TreeElement cache() {
		if(recordId == TreeReference.INDEX_TEMPLATE) {
			return empty;
		}
		synchronized(parent.treeCache){
			TreeElement element = parent.treeCache.retrieve(recordId);
			if(element != null) {
				return element;
			}
			
			TreeElement cacheBuilder = new TreeElement("stock"); 
			Stock s = parent.storage.read(recordId);
			entityId = s.getEntiyId();
			cacheBuilder = new TreeElement("stock");
			cacheBuilder.setMult(this.mult);
			
			cacheBuilder.setAttribute(null, "id", s.getEntiyId());
		
			TreeElement product;

			String[] productList =  s.getProductList();
			for(int i = 0 ; i < productList.length ; ++i) {
				product = new TreeElement("product", i);
				product.setAttribute(null, "id", productList[i]);
				product.setValue(new IntegerData(s.getProductValue(productList[i])));
				cacheBuilder.addChild(product);
			}
			
			cacheBuilder.setParent(this.parent);
			
			parent.treeCache.register(recordId, cacheBuilder);
			
			return cacheBuilder;
		}
	}

	public boolean isRelevant() {
		return true;
	}

	public static StockChildElement TemplateElement(StockInstanceTreeElement parent) {
		StockChildElement template = new StockChildElement(parent);
		return template;
	}

	public Vector<TreeReference> tryBatchChildFetch(String name, int mult, Vector<XPathExpression> predicates, EvaluationContext evalContext) {
		//TODO: We should be able to catch the index case here?
		return null;
	}

	public String getNamespace() {
		return null;
	}

}
