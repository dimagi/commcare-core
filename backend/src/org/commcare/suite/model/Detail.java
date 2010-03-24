/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

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
	
	private FormInstance context;
	
	private Filter filter;
	
	private Vector<Text> headers;
	private Vector<Text> templates;
	
	private int[] headerHints;
	private int[] templateHints;
	
	/**
	 * Serialization Only
	 */
	public Detail() {
		
	}
	
	public Detail(String id, Text title, FormInstance context, Vector<Text> headers, Vector<Text> templates, Filter filter) {
		this.id = id;
		this.title = title;
		this.context = context;
		this.headers = headers;
		this.templates = templates;
		this.filter = filter;
		this.headerHints = initBlank(headers.size());
		this.templateHints = initBlank(templates.size());
	}
	
	public Detail(String id, Text title, FormInstance context, Vector<Text> headers, Vector<Text> templates, Filter filter, int[] headerHints, int[] templateHints) {
		this.id = id;
		this.title = title;
		this.context = context;
		this.headers = headers;
		this.templates = templates;
		this.filter = filter;
		this.headerHints = headerHints;
		this.templateHints = templateHints;
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
	
	/**
	 * @return A data model which describes the format of data which
	 * is expected by the templates in this detail definition. This
	 * model should be filled by the application and then passed to
	 * the templated text items to display data.
	 */
	public FormInstance getInstance() {
		return context;
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
		context = (FormInstance)ExtUtil.read(in, FormInstance.class, pf);
		headers = (Vector<Text>)ExtUtil.read(in, new ExtWrapList(Text.class), pf);
		templates = (Vector<Text>)ExtUtil.read(in, new ExtWrapList(Text.class), pf);
		headerHints = (int[])ExtUtil.readInts(in);
		templateHints = (int[])ExtUtil.readInts(in);
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out,id);
		ExtUtil.write(out, title);
		ExtUtil.write(out, filter);
		ExtUtil.write(out, context);
		ExtUtil.write(out, new ExtWrapList(headers));
		ExtUtil.write(out, new ExtWrapList(templates));
		ExtUtil.writeInts(out, headerHints);
		ExtUtil.writeInts(out, templateHints);
	}
	
}
