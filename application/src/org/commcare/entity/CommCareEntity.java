/**
 * 
 */
package org.commcare.entity;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.entity.model.Entity;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;

/**
 * @author ctsims
 *
 */
public class CommCareEntity extends Entity<TreeReference> {

	Detail shortDetail;
	Detail longDetail;
	String[] shortText;
	String[] sortText;
	EvaluationContext context;
	NodeEntitySet set;
	
	public CommCareEntity(Detail shortDetail, Detail longDetail, EvaluationContext context, NodeEntitySet set) {
		this.shortDetail = shortDetail;
		this.longDetail = longDetail;
		this.context = context;
		this.set = set;
	}
	
	protected int readEntityId(TreeReference element) {
		return set.getId(element);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#entityType()
	 */
	public String entityType() {
		return shortDetail.getTitle().evaluate(context);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#factory()
	 */
	public Entity<TreeReference> factory() {
		CommCareEntity entity = new CommCareEntity(shortDetail,longDetail, context, set);
		return entity;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getHeaders(boolean)
	 */
	public String[] getHeaders(boolean detailed) {
		Detail d;
		if(!detailed) {
			d = shortDetail;
		} else{
			if(longDetail == null) { return null;}
			d = longDetail;
		}
		
		String[] output = new String[d.getFields().length];
		for(int i = 0 ; i < output.length ; ++i) {
			output[i] = d.getFields()[i].getHeader().evaluate();
		}
		return output;
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.patient.select.activity.IEntity#matchID(java.lang.String)
	 */
	public boolean match (String key) {
		key = key.toLowerCase();
		String[] fields = this.getShortFields();
		for(int i = 0; i < fields.length; ++i) {
			//don't match to images
			if("image".equals(shortDetail.getFields()[i].getTemplateForm())) {
				continue;
			}
			if(fields[i].toLowerCase().startsWith(key)) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getForms(boolean)
	 */
	public String[] getForms(boolean header) {
		return header ? shortDetail.getHeaderForms() : shortDetail.getTemplateForms();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getForms(boolean)
	 */
	public String[] getLongForms(boolean header) {
		if(longDetail == null) { return null;}
		return header ? longDetail.getHeaderForms() : longDetail.getTemplateForms();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getLongFields(org.javarosa.core.services.storage.Persistable)
	 */
	public String[] getLongFields(TreeReference element) {
		if(longDetail == null) { return null;}
		EvaluationContext ec = new EvaluationContext(context, element);
		loadVars(ec, longDetail);
		String[] output = new String[longDetail.getFields().length];
		for(int i = 0 ; i < output.length ; ++i) {
			output[i] = longDetail.getFields()[i].getTemplate().evaluate(ec);
		}
		return output;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getShortFields()
	 */
	public String[] getShortFields() {
		return shortText;
	}
	
	public int[] getStyleHints (boolean header) {
		if(header) {
			return shortDetail.getHeaderSizeHints();
		} else {
			return shortDetail.getTemplateSizeHints();
		}
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#loadEntity(org.javarosa.core.services.storage.Persistable)
	 */
	protected void loadEntity(TreeReference element) {
		EvaluationContext ec = new EvaluationContext(context, element);
		loadVars(ec, shortDetail);
		loadTexts(ec);
	}
	
	private void loadVars(EvaluationContext ec, Detail detail) {
		Hashtable<String, XPathExpression> decs = detail.getVariableDeclarations();
		for(Enumeration en = decs.keys() ; en.hasMoreElements();) {
			String key = (String)en.nextElement();
			try {
				ec.setVariable(key, XPathFuncExpr.unpack(decs.get(key).eval(ec)));
			} catch(XPathException xpe) {
				xpe.printStackTrace();
				throw new RuntimeException("XPathException while parsing varible " + key+ " for entity. " +  xpe.getMessage());
			}
		}
	}
	
	private void loadTexts(EvaluationContext context) {
		shortText = new String[shortDetail.getFields().length];
		sortText = new String[shortDetail.getFields().length];
		for(int i = 0 ; i < shortText.length ; ++i) {
			shortText[i] = shortDetail.getFields()[i].getTemplate().evaluate(context);
			
			//see whether or not the field has a special text form just for sorting
			Text sortKey = shortDetail.getFields()[i].getSort();
			if(sortKey == null) {
				//If not, we'll just use the display text
				sortText[i] = shortText[i];
			} else {
				//If so, evaluate it
				sortText[i] = sortKey.evaluate(context);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getSortFields()
	 */
	public String[] getSortFields () {
		int[] sortOrder = shortDetail.getSortOrder();
		int topIndex = sortOrder.length == 0 ? -1 : sortOrder[0];
		Vector<String> fields = new Vector<String>();
		if(topIndex != -1) {
			fields.addElement(String.valueOf(topIndex));
		}
		fields.addElement("DEFAULT");
		String[] headers = getHeaders(false);
		for(int i = 0 ; i < headers.length ; ++i) {
			if(headers[i] == null  || headers[i].equals("")) { continue;}
			if(i == topIndex) {
				//nothing
			} else {
				fields.addElement(String.valueOf(i));
			}
		}
		String[] ret = new String[fields.size()];
		for(int i = 0; i < fields.size() ; ++i) {
			ret[i] = fields.elementAt(i);
		}
		return ret;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getSortFieldNames()
	 */
	public String[] getSortFieldNames () {
		String[] headers = getHeaders(false);
		String[] sortKeys = getSortFields();
		String[] ret = new String[sortKeys.length];
		
		for(int i = 0 ; i < ret.length ; ++i) {
			if(sortKeys[i].equals("DEFAULT")) {
				ret[i] = Localization.get("case.id");
			} else {
				try{
					ret[i] = headers[Integer.valueOf(sortKeys[i]).intValue()];
				} catch(NumberFormatException nfe) {
					nfe.printStackTrace();
					throw new RuntimeException("Invalid sort key in CommCare Entity: " + sortKeys[i]);
				}
			}
			
		}
		return ret;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getSortKey(java.lang.String)
	 */
	public Object getSortKey (String fieldKey) {
		if (fieldKey.equals("DEFAULT")) {
			return DataUtil.integer(this.getRecordID());
		} else {
			try{
				return sortText[Integer.valueOf(fieldKey).intValue()];
			} catch(NumberFormatException nfe) {
				nfe.printStackTrace();
				throw new RuntimeException("Invalid sort key in CommCare Entity: " + fieldKey);
			}
		}
	}
	
	/**
	 * Returns the orientation that the specified field should be sorted in 
	 * 
	 * @param fieldKey The key to be sorted
	 * @return true if the field should be sorted in ascending order. False otherwise
	 */
	public boolean isSortAscending(String fieldKey) {
		if (fieldKey.equals("DEFAULT")) {
			return true;
		} else {
			try{
				return !(shortDetail.getFields()[Integer.parseInt(fieldKey)].getSortDirection() == DetailField.DIRECTION_DESCENDING);
			} catch(NumberFormatException nfe) {
				nfe.printStackTrace();
				throw new RuntimeException("Invalid sort key in CommCare Entity: " + fieldKey);
			}
		}
	}
}
