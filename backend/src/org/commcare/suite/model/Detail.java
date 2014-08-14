/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import org.commcare.util.GridCoordinate;
import org.commcare.util.GridStyle;
import org.javarosa.core.util.ArrayUtilities;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

/**
 * <p>A Detail model defines the structure in which
 * the details about something should be displayed
 * to users (generally cases or referrals).</p>
 * 
 * <p>Detail models maintain a set of Text objects
 * which provide a template for how details about 
 * objects should be displayed, along with a model
 * which defines the context of what data should be 
 * obtained to fill in those templates.</p>
 * 
 * @author ctsims
 *
 */
public class Detail implements Externalizable {
	
	private String id;
	
	private Text title;
	
	DetailField[] fields;
	
	OrderedHashtable<String, String> variables;
	OrderedHashtable<String, XPathExpression> variablesCompiled;
	
	/**
	 * Serialization Only
	 */
	public Detail() {
		
	}
	
	public Detail(String id, Text title, Vector<DetailField> fields, OrderedHashtable<String, String> variables) {
		this(id, title, ArrayUtilities.copyIntoArray(fields, new DetailField[fields.size()]), variables);
	}
	
	public Detail(String id, Text title, DetailField[] fields, OrderedHashtable<String, String> variables) {
		this.id = id;
		this.title = title;
		this.fields = fields;
		this.variables = variables;
	}
	
	private int[] initBlank(int size) {
		int[] blank = new int[size];
		for(int i = 0; i < size ; ++i) {
			blank[i] = -1;
		}
		return blank;
	}
	
	/**
	 * @return The id of this detail template
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * @return A title to be displayed to users regarding
	 * the type of content being described.
	 */
	public Text getTitle() {
		return title;
	}
	public DetailField[] getFields() {
		return fields;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		id = ExtUtil.readString(in);
		title = (Text)ExtUtil.read(in, Text.class, pf);
		Vector<DetailField> theFields  = (Vector<DetailField>)ExtUtil.read(in, new ExtWrapList(DetailField.class), pf);
		fields = new DetailField[theFields.size()];
		ArrayUtilities.copyIntoArray(theFields, fields);
		variables = (OrderedHashtable<String, String>)ExtUtil.read(in, new ExtWrapMap(String.class, String.class, ExtWrapMap.TYPE_SLOW_READ_ONLY));
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out,id);
		ExtUtil.write(out, title);
		ExtUtil.write(out, new ExtWrapList(ArrayUtilities.toVector(fields)));
		ExtUtil.write(out, new ExtWrapMap(variables));
	}
	
	public OrderedHashtable<String, XPathExpression> getVariableDeclarations() {
		if(variablesCompiled == null) {
			variablesCompiled = new OrderedHashtable<String, XPathExpression>();
			for(Enumeration en = variables.keys(); en.hasMoreElements() ; ) {
				String key = (String)en.nextElement();
				//TODO: This is stupid, parse this stuff at XML Parse time.
				try {
					variablesCompiled.put(key, XPathParseTool.parseXPath(variables.get(key)));
				} catch (XPathSyntaxException e) {
					e.printStackTrace();
					throw new RuntimeException(e.getMessage());
				}
			}
		}
		return variablesCompiled;
	}
	
	public Vector<String> toVector(String[] array) {
		Vector<String> ret = new Vector<String>();
		for(String s : array) {
			ret.addElement(ExtUtil.emptyIfNull(s));
		}
		return ret;
	}
	
	public String[] toArray(Vector<String> v) {
		String[] a = new String[v.size()];
		for(int i = 0; i < a.length ; ++i ){
			a[i] = ExtUtil.nullIfEmpty(v.elementAt(i));
		}
		return a;
	}
	
	/**
	 * @return The indices of which fields should be used for sorting and their order
	 */
	public int[] getSortOrder() {
		Vector<Integer> indices = new Vector<Integer>();
		outer:
		for(int i = 0 ; i < fields.length ; ++i) {
			int order = fields[i].getSortOrder();
			if(order < 1) { continue;}
			for(int j = 0 ; j < indices.size() ; ++j) {
				if(order < fields[indices.elementAt(j).intValue()].getSortOrder()) {
					indices.insertElementAt(new Integer(i), j);
					continue outer;
				}
			}
			//otherwise it's larger than all of the other fields.
			indices.addElement(new Integer(i));
			continue;
		}
		if(indices.size() == 0) { return new int[] {};}
		else {
			int[] ret = new int[indices.size()];
			for(int i = 0 ; i < ret.length ; ++i) {
				ret[i] = indices.elementAt(i).intValue();
			}
			return ret;
		}
	}
	
	//These are just helpers around the old structure. Shouldn't really be
	//used if avoidable
	
	
	/**
	 * Obsoleted - Don't use
	 */
	public int[] getHeaderSizeHints() {
		return new Map<int[]>(new int[fields.length]) {
			protected void map(DetailField f, int[] a, int i) {
				a[i] = f.getHeaderHint();
			}
		}.go();
	}

	/**
	 * Obsoleted - Don't use
	 */
	public int[] getTemplateSizeHints() {
		return new Map<int[]>(new int[fields.length]) {
			protected void map(DetailField f, int[] a, int i) {
				a[i] = f.getTemplateHint();
			}
		}.go();
	}

	/**
	 * Obsoleted - Don't use
	 */
	public String[] getHeaderForms() {
		return new Map<String[]>(new String[fields.length]) {
			protected void map(DetailField f, String[] a, int i) {
				a[i] = f.getHeaderForm();
			}
		}.go();
	}

	/**
	 * Obsoleted - Don't use
	 */
	public String[] getTemplateForms() {
		return new Map<String[]>(new String[fields.length]) {
			protected void map(DetailField f, String[] a, int i) {
				a[i] = f.getTemplateForm();
			}
		}.go();
	}
	
	public boolean usesGridView(){
		
		boolean usesGrid = false;
		
		for(int i=0; i< fields.length; i++){
			DetailField currentField = fields[i];
			if(currentField.getGridX() >= 0 && currentField.getGridY() >= 0 &&
					currentField.getGridWidth() >= 0 && currentField.getGridHeight() > 0){
				usesGrid = true;
			}
		}
		
		return usesGrid;
	}
	
	public GridCoordinate[] getGridCoordinates(){
		GridCoordinate [] mGC = new GridCoordinate[fields.length];
		
		for(int i=0; i< fields.length; i++){
			DetailField currentField = fields[i];
			mGC[i] = new GridCoordinate(currentField.getGridX(), currentField.getGridY(),
											currentField.getGridWidth(), currentField.getGridHeight());
		}
		
		return mGC;
	}
	
	public GridStyle[] getGridStyles(){
		GridStyle [] mGC = new GridStyle[fields.length];
		
		for(int i=0; i< fields.length; i++){
			DetailField currentField = fields[i];
			mGC[i] = new GridStyle(currentField.getFontSize(), currentField.getHorizontalAlign(), 
											currentField.getVerticalAlign(), currentField.getCssId());
		}
		
		return mGC;
	}
	
	private abstract class Map<E> {
		private E a;
		private Map(E a) { this.a = a; }  
		protected abstract void map(DetailField f, E a, int i);
		public E go() {
			for(int i = 0 ; i < fields.length ; ++i){
				map(fields[i], a, i);
			}
			return a;
		}
	}
}
