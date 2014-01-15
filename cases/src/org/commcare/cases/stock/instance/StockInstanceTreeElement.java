/**
 * 
 */
package org.commcare.cases.stock.instance;

import java.util.Hashtable;
import java.util.Vector;

import org.commcare.cases.stock.Stock;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.CacheTable;
import org.javarosa.core.util.DataUtil;
import org.javarosa.xpath.expr.XPathExpression;

/**
 * The root element for the <casedb> abstract type. All children are
 * nodes in the case database. Depending on instantiation, the <casedb>
 * may include only a subset of the full db. 
 * 
 * @author ctsims
 *
 */
public class StockInstanceTreeElement implements AbstractTreeElement<StockChildElement> {

	private AbstractTreeElement instanceRoot;
	
	IStorageUtilityIndexed<Stock> storage;
	private String[] stockRecords;
	
	private Vector<StockChildElement> stocks;
	
	protected CacheTable<TreeElement> treeCache = new CacheTable<TreeElement>();
	
	protected CacheTable<String> stringCache;
	
	private Hashtable<Integer, Integer> stockIdMapping;
	
	public StockInstanceTreeElement(AbstractTreeElement instanceRoot, IStorageUtilityIndexed storage) {
		this.instanceRoot= instanceRoot;
		this.storage = storage;
		storage.setReadOnly();
	}
	
	public void rebase(AbstractTreeElement instanceRoot) {
		this.instanceRoot = instanceRoot;
		expireCachedRef();
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
	
	public void attachStringCache(CacheTable<String> stringCache) {
		this.stringCache = stringCache;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getChild(java.lang.String, int)
	 */
	public StockChildElement getChild(String name, int multiplicity) {
		if(multiplicity == TreeReference.INDEX_TEMPLATE) {
			return null;
		}
		
		//name is always the same, so multiplicities are the only relevant component here
		if(name.equals(StockChildElement.NAME)) { 
			getStocks();
			if(stocks.size() == 0) {
				//If we have no cases, we still need to be able to return a template element so as to not
				//break xpath evaluation
				return StockChildElement.TemplateElement(this);
			}
			return stocks.elementAt(multiplicity);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildrenWithName(java.lang.String)
	 */
	public Vector getChildrenWithName(String name) {
		if(name.equals(StockChildElement.NAME)) {
			getStocks();
			return stocks;
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
		if(stockRecords != null) {
			return stockRecords.length;
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
	public StockChildElement getChildAt(int i) {
		getStocks();
		return stocks.elementAt(i);
	}
	
	private synchronized void getStocks() {
		if(stocks != null) {
			return;
		}
		stockIdMapping = new Hashtable<Integer, Integer>();
		stocks = new Vector<StockChildElement>();
		if(stockRecords != null) {
			int i = 0;
			for(String id : stockRecords) {
				stocks.addElement(new StockChildElement(this, -1, id, i));
				++i;
			}
		} else {
			int mult = 0;
			for(IStorageIterator i = storage.iterate(); i.hasMore();) {
				int id = i.nextID();
				stocks.addElement(new StockChildElement(this, id, null, mult));
				stockIdMapping.put(DataUtil.integer(id), DataUtil.integer(mult));
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
		if(name.equals(StockChildElement.NAME)) {
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
	public StockChildElement getAttribute(String namespace, String name) {
		//Oooooof, this is super janky;
		return null;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(java.lang.String, java.lang.String)
	 */
	public String getAttributeValue(String namespace, String name) {
		return null;
	}
	
	TreeReference cachedRef = null;
	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getRef()
	 */
	public TreeReference getRef() {
		if(cachedRef ==null) {
			cachedRef = TreeElement.BuildRef(this);
		}
		return cachedRef;
	}
	
	private void expireCachedRef() {
		cachedRef = null;
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
		return "stockdb";
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

	public Vector<TreeReference> tryBatchChildFetch(String name, int mult, Vector<XPathExpression> predicates, EvaluationContext evalContext) {
		return null;
	}

//	protected Vector<Integer> union(Vector<Integer> selectedCases, Vector<Integer> cases) {
//		return DataUtil.union(selectedCases, cases);
//	}

	public String getNamespace() {
		return null;
	}

	public String intern(String s) {
		if(stringCache == null) {
			return s;
		} else {
			return stringCache.intern(s);
		}
	}
}
