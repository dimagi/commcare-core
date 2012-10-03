/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class DetailField implements Externalizable {
	
	public static final int DIRECTION_ASCENDING = 1;
	public static final int DIRECTION_DESCENDING = 2;
	
	private Text header;
	private Text template; 
	private Text sort; 
	private int headerHint = -1; 
	private int templateHint = -1; 
	private String headerForm; 
	private String templateForm; 
	private int sortOrder = -1;
	private int sortDirection = DIRECTION_ASCENDING;
	
	public DetailField() {
		
	}
	
	public DetailField(Text header, Text template, Text sort, int headerHint, int templateHint, String headerForm, String templateForm, int sortOrder, int sortDirection) {
		this.header = header;
		this.template = template;
		this.sort = sort;
		this.headerHint = headerHint;
		this.templateHint = templateHint;
		this.headerForm = headerForm;
		this.templateForm = templateForm;
		this.sortOrder = sortOrder;
		this.sortDirection = sortDirection;
	}
	
	
	/**
	 * @return the header
	 */
	public Text getHeader() {
		return header;
	}


	/**
	 * @return the template
	 */
	public Text getTemplate() {
		return template;
	}


	/**
	 * @return the sort
	 */
	public Text getSort() {
		return sort;
	}



	/**
	 * @return the headerHint
	 */
	public int getHeaderHint() {
		return headerHint;
	}



	/**
	 * @return the templateHint
	 */
	public int getTemplateHint() {
		return templateHint;
	}



	/**
	 * @return the headerForm
	 */
	public String getHeaderForm() {
		return headerForm;
	}



	/**
	 * @return the templateForm
	 */
	public String getTemplateForm() {
		return templateForm;
	}



	/**
	 * @return the sortOrder
	 */
	public int getSortOrder() {
		return sortOrder;
	}



	/**
	 * @return the sortDirection
	 */
	public int getSortDirection() {
		return sortDirection;
	}



	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		header = (Text)ExtUtil.read(in, Text.class);
		template = (Text)ExtUtil.read(in, Text.class);
		sort = (Text)ExtUtil.read(in, new ExtWrapNullable(Text.class));
		headerHint = ExtUtil.readInt(in);
		templateHint = ExtUtil.readInt(in);
		headerForm = ExtUtil.readString(in);
		templateForm = ExtUtil.readString(in);
		sortOrder = ExtUtil.readInt(in);
		sortDirection = ExtUtil.readInt(in);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.write(out, header);
		ExtUtil.write(out, template);
		ExtUtil.write(out, new ExtWrapNullable(sort));
		ExtUtil.writeNumeric(out, headerHint);
		ExtUtil.writeNumeric(out, templateHint);
		ExtUtil.writeString(out,headerForm);
		ExtUtil.writeString(out,templateForm);
		ExtUtil.writeNumeric(out, sortOrder);
		ExtUtil.writeNumeric(out, sortDirection);
	}
	
	public class Builder {
		DetailField field;
		public Builder() {
			field = new DetailField();
		}
		
		public DetailField build() {
			return field;
		}

		/**
		 * @param header the header to set
		 */
		public void setHeader(Text header) {
			field.header = header;
		}



		/**
		 * @param template the template to set
		 */
		public void setTemplate(Text template) {
			field.template = template;
		}



		/**
		 * @param sort the sort to set
		 */
		public void setSort(Text sort) {
			field.sort = sort;
		}



		/**
		 * @param headerHint the headerHint to set
		 */
		public void setHeaderHint(int headerHint) {
			field.headerHint = headerHint;
		}



		/**
		 * @param templateHint the templateHint to set
		 */
		public void setTemplateHint(int templateHint) {
			field.templateHint = templateHint;
		}



		/**
		 * @param headerForm the headerForm to set
		 */
		public void setHeaderForm(String headerForm) {
			field.headerForm = headerForm;
		}



		/**
		 * @param templateForm the templateForm to set
		 */
		public void setTemplateForm(String templateForm) {
			field.templateForm = templateForm;
		}



		/**
		 * @param sortOrder the sortOrder to set
		 */
		public void setSortOrder(int sortOrder) {
			field.sortOrder = sortOrder;
		}



		/**
		 * @param sortDirection the sortDirection to set
		 */
		public void setSortDirection(int sortDirection) {
			field.sortDirection = sortDirection;
		}
	}
}
