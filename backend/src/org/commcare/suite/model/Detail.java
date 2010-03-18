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
 * @author ctsims
 *
 */
public class Detail implements Externalizable {
	String id;
	
	Text title;
	
	FormInstance context;
	
	Filter filter;
	
	Vector<Text> headers;
	Vector<Text> templates;
	
	int[] headerHints;
	int[] templateHints;
	
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
	
	public String getId() {
		return id;
	}
	
	public Text getTitle() {
		return title;
	}
	
	public Text[] getHeaders() {
		Text[] array = new Text[headers.size()];
		headers.copyInto(array);
		return array;
	}
	
	public Text[] getTemplates() {
		Text[] array = new Text[templates.size()];
		templates.copyInto(array);
		return array;
	}
	
	public int[] getHeaderSizeHints() {
		return headerHints;
	}
	
	public int[] getTemplateSizeHints() {
		return templateHints;
	}
	
	public FormInstance getInstance() {
		return context;
	}
	
	public Filter getFilter() {
		return filter;
	}

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
