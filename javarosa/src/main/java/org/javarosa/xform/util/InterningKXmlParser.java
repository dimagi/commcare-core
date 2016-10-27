/**
 *
 */
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

    /* (non-Javadoc)
     * @see org.kxml2.io.KXmlParser#getAttributeName(int)
     */
    @Override
    public String getAttributeName(int arg0) {
        return stringCache.intern(super.getAttributeName(arg0));

    }

    /* (non-Javadoc)
     * @see org.kxml2.io.KXmlParser#getAttributeNamespace(int)
     */
    @Override
    public String getAttributeNamespace(int arg0) {
        return stringCache.intern(super.getAttributeNamespace(arg0));

    }

    /* (non-Javadoc)
     * @see org.kxml2.io.KXmlParser#getAttributePrefix(int)
     */
    @Override
    public String getAttributePrefix(int arg0) {
        return stringCache.intern(super.getAttributePrefix(arg0));
    }

    /* (non-Javadoc)
     * @see org.kxml2.io.KXmlParser#getAttributeValue(int)
     */
    @Override
    public String getAttributeValue(int arg0) {
        return stringCache.intern(super.getAttributeValue(arg0));

    }

    /* (non-Javadoc)
     * @see org.kxml2.io.KXmlParser#getNamespace(java.lang.String)
     */
    @Override
    public String getNamespace(String arg0) {
        return stringCache.intern(super.getNamespace(arg0));

    }

    /* (non-Javadoc)
     * @see org.kxml2.io.KXmlParser#getNamespaceUri(int)
     */
    @Override
    public String getNamespaceUri(int arg0) {
        return stringCache.intern(super.getNamespaceUri(arg0));
    }

    /* (non-Javadoc)
     * @see org.kxml2.io.KXmlParser#getText()
     */
    @Override
    public String getText() {
        return stringCache.intern(super.getText());

    }

    @Override
    public String getName() {
        return stringCache.intern(super.getName());
    }
}
