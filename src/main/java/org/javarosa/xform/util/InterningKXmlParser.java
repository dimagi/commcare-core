package org.javarosa.xform.util;

import org.javarosa.core.util.Interner;
import org.kxml2.io.KXmlParser;

/**
 * @author ctsims
 */
public class InterningKXmlParser extends KXmlParser {

    final Interner<String> stringCache;

    public InterningKXmlParser(Interner<String> stringCache) {
        super();
        this.stringCache = stringCache;
    }

    public void release() {
        //Anything?
    }

    @Override
    public String getAttributeName(int arg0) {
        return stringCache.intern(super.getAttributeName(arg0));

    }

    @Override
    public String getAttributeNamespace(int arg0) {
        return stringCache.intern(super.getAttributeNamespace(arg0));

    }

    @Override
    public String getAttributePrefix(int arg0) {
        return stringCache.intern(super.getAttributePrefix(arg0));
    }

    @Override
    public String getAttributeValue(int arg0) {
        return stringCache.intern(super.getAttributeValue(arg0));

    }

    @Override
    public String getNamespace(String arg0) {
        return stringCache.intern(super.getNamespace(arg0));

    }

    @Override
    public String getNamespaceUri(int arg0) {
        return stringCache.intern(super.getNamespaceUri(arg0));
    }

    @Override
    public String getText() {
        return stringCache.intern(super.getText());

    }

    @Override
    public String getName() {
        return stringCache.intern(super.getName());
    }
}
