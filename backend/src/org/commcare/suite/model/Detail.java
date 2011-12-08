/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
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
	
	private Filter filter;
	
	private Vector<Text> headers;
	private Vector<Text> templates;
	
	private int[] headerHints;
	private int[] templateHints;
	
	private String[] headerForms;
	private String[] templateForms;
	
	private int defaultSort;
	
	Hashtable<String, String> variables;
	Hashtable<String, XPathExpression> variablesCompiled;
	
	/**
	 * Serialization Only
	 */
	public Detail() {
		
	}
	
	public Detail(String id, Text title, Vector<Text> headers, Vector<Text> templates, Filter filter, int defaultSort, Hashtable<String, String> variables) {
		this.id = id;
		this.title = title;
		this.headers = headers;
		this.templates = templates;
		this.filter = filter;
		this.headerHints = initBlank(headers.size());
		this.templateHints = initBlank(templates.size());
		this.headerForms = new String[headers.size()];
		this.templateForms = new String[templates.size()];
		this.defaultSort = defaultSort;
		this.variables = variables;
	}
	
	public Detail(String id, Text title, Vector<Text> headers, Vector<Text> templates, Filter filter, int[] headerHints, int[] templateHints, int defaultSort, Hashtable<String, String> variables) {
		this(id,title,headers,templates,filter, defaultSort, variables);
		this.headerHints = headerHints;
		this.templateHints = templateHints;
	}
	
	public Detail(String id, Text title, Vector<Text> headers, Vector<Text> templates, Filter filter, int[] headerHints, int[] templateHints, String[] headerForms, String[] templateForms, int defaultSort, Hashtable<String, String> variables) {
		this(id,title,headers,templates,filter,headerHints,templateHints, defaultSort, variables);
		this.headerForms = headerForms;
		this.templateForms = templateForms;
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
	
	/**
	 * @return A set of header text to be displayed for
	 * each piece of data which is describe by a template.
	 */
	public Text[] getHeaders() {
		Text[] array = new Text[headers.size()];
		headers.copyInto(array);
		return array;
	}
	
	/**
	 * @return A set of Text definitions which provide
	 * a template for how to display the object in a data
	 * model.
	 */
	public Text[] getTemplates() {
		Text[] array = new Text[templates.size()];
		templates.copyInto(array);
		return array;
	}
	
	/**
	 * @return An array of integers which are either -1 or
	 * between 0 and 100 defining a hint for what % of the screen
	 * should be used to display each header at the appropriate
	 * index.
	 */
	public int[] getHeaderSizeHints() {
		return headerHints;
	}
	
	/**
	 * @return An array of integers which are either -1 or
	 * between 0 and 100 defining a hint for what % of the screen
	 * should be used to display each piece of templated text at 
	 * the appropriate index.
	 */
	public int[] getTemplateSizeHints() {
		return templateHints;
	}
	
	public String[] getHeaderForms() {
		return headerForms;
	}
	
	public String[] getTemplateForms() {
		return templateForms;
	}
	
	/**
	 * @return a Filter object which can be used to determine what 
	 * data elements this detail definition can describe.
	 */
	public Filter getFilter() {
		return filter;
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		id = ExtUtil.readString(in);
		title = (Text)ExtUtil.read(in, Text.class, pf);
		filter = (Filter)ExtUtil.read(in, Filter.class, pf);
		headers = (Vector<Text>)ExtUtil.read(in, new ExtWrapList(Text.class), pf);
		templates = (Vector<Text>)ExtUtil.read(in, new ExtWrapList(Text.class), pf);
		headerHints = (int[])ExtUtil.readInts(in);
		templateHints = (int[])ExtUtil.readInts(in);
		headerForms = toArray((Vector<String>)ExtUtil.read(in, new ExtWrapList(String.class), pf));
		templateForms = toArray((Vector<String>)ExtUtil.read(in, new ExtWrapList(String.class), pf));
		defaultSort = ExtUtil.readInt(in);
		variables = (Hashtable<String, String>)ExtUtil.read(in, new ExtWrapMap(String.class, String.class));
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out,id);
		ExtUtil.write(out, title);
		ExtUtil.write(out, filter);
		ExtUtil.write(out, new ExtWrapList(headers));
		ExtUtil.write(out, new ExtWrapList(templates));
		ExtUtil.writeInts(out, headerHints);
		ExtUtil.writeInts(out, templateHints);
		ExtUtil.write(out, new ExtWrapList(toVector(headerForms)));
		ExtUtil.write(out, new ExtWrapList(toVector(templateForms)));
		ExtUtil.writeNumeric(out, defaultSort);
		ExtUtil.write(out, new ExtWrapMap(variables));
	}
	
	public Hashtable<String, XPathExpression> getVariableDeclarations() {
		if(variablesCompiled == null) {
			variablesCompiled = new Hashtable<String, XPathExpression>();
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
	
	public int getDefaultSort() {
		//Sort order keys
		return defaultSort;
	}
	
}
