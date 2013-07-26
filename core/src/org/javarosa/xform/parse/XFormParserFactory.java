package org.javarosa.xform.parse;

import java.io.Reader;

import org.javarosa.core.util.CacheTable;
import org.kxml2.kdom.Document;

/**
 * Class factory for creating an XFormParser.
 * Supports experimental extensions of XFormParser.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class XFormParserFactory implements IXFormParserFactory {
	CacheTable<String> stringCache;

	public XFormParserFactory() {
	}
	
	public XFormParserFactory(CacheTable<String> stringCache) {
		this.stringCache = stringCache;
	}
	
	public XFormParser getXFormParser(Reader reader) {
		XFormParser parser = new XFormParser(reader);
		if(stringCache != null) {
			parser.setStringCache(stringCache);
		}
		return parser;
	}
	
	public XFormParser getXFormParser(Document doc) {
		XFormParser parser = new XFormParser(doc);
		if(stringCache != null) {
			parser.setStringCache(stringCache);
		}
		return parser;
	}
	
	public XFormParser getXFormParser(Reader form, Reader instance) {
		XFormParser parser = new XFormParser(form, instance);
		if(stringCache != null) {
			parser.setStringCache(stringCache);
		}
		return parser;
	}
	
	public XFormParser getXFormParser(Document form, Document instance) {
		XFormParser parser = new XFormParser(form, instance);
		if(stringCache != null) {
			parser.setStringCache(stringCache);
		}
		return parser;
	}

}