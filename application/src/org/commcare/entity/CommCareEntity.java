/**
 * 
 */
package org.commcare.entity;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.Detail;
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
		return shortDetail.getTitle().evaluate();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#factory()
	 */
	public Entity<TreeReference> factory() {
		return new CommCareEntity(shortDetail,longDetail, context, set);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getHeaders(boolean)
	 */
	public String[] getHeaders(boolean detailed) {
		Text[] text;
		if(!detailed) {
			text = shortDetail.getHeaders();
		} else{
			if(longDetail == null) { return null;}
			text = longDetail.getHeaders();
		}
		
		String[] output = new String[text.length];
		for(int i = 0 ; i < output.length ; ++i) {
			output[i] = text[i].evaluate();
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
			//Skip sorting by this key if it's not a normal string
			if("image".equals(shortDetail.getTemplateForms()[i])) {
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
		Text[] text = longDetail.getTemplates();
		String[] output = new String[text.length];
		for(int i = 0 ; i < output.length ; ++i) {
			output[i] = text[i].evaluate(ec);
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
		loadShortText(ec);
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
	
	private void loadShortText(EvaluationContext context) {
		Text[] text = shortDetail.getTemplates();
		shortText = new String[text.length];
		for(int i = 0 ; i < shortText.length ; ++i) {
			shortText[i] = text[i].evaluate(context);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getSortFields()
	 */
	public String[] getSortFields () {
		String[] names = getSortFieldNames();
		String[] ret = new String[names.length];
		int defaultSort = getSortFieldDefault();
		
		if(defaultSort != -1) {
			ret[0] = String.valueOf(defaultSort);
			ret[1] = "DEFAULT";
		}
		else {
			ret[0] = "DEFAULT";
		}
		int position = defaultSort == -1 ? 1 : 2;
		for(int i = 1; i < ret.length ; ++i ) {
			if(defaultSort != i) {
				ret[position] = String.valueOf(i);
				position++;
			}
		}
		return ret;
	}
	
	private int getSortFieldDefault() {
		int topIndex = shortDetail.getDefaultSort();
		if(topIndex == -1) { return -1; }
		
		int index = -1;
		Vector<String> fields = new Vector<String>();
		fields.addElement(Localization.get("case.id"));
		String[] headers = getHeaders(false);
		for(int i = 0 ; i < headers.length ; ++i) {
			if(headers[i] == null  || headers[i].equals("")) { continue;}
			fields.addElement(headers[i]);
			if(i == topIndex) {
				return fields.size() - 1;
			}
		}
		return -1;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.entity.model.Entity#getSortFieldNames()
	 */
	public String[] getSortFieldNames () {
		Vector<String> fields = new Vector<String>();
		int sortField = this.getSortFieldDefault();
		if(sortField != -1) {
			sortField --;
		}
		String[] headers = getHeaders(false);
		
		if(sortField == -1) {
			fields.addElement(Localization.get("case.id"));
		} else {
			fields.addElement(headers[sortField]);
			fields.addElement(Localization.get("case.id"));
		}
		
		for(int i = 0; i < headers.length; ++i) {
			if(i == sortField) { continue; }
			String s = headers[i];
			if(s == null || "".equals(s)) {
				continue;
			}
			fields.addElement(s);
		}
		String[] ret = new String[fields.size()];
		for(int i = 0 ; i < ret.length ; ++i) {
			ret[i] = fields.elementAt(i);
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
				return getShortFields()[Integer.valueOf(fieldKey).intValue() - 1];
			} catch(NumberFormatException nfe) {
				nfe.printStackTrace();
				throw new RuntimeException("Invalid sort key in CommCare Entity: " + fieldKey);
			}
		}
	}
}
