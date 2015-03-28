package org.javarosa.xpath;

/**
 * Created by wpride1 on 3/28/15.
 */
public class XPathArityException extends XPathException {
    public XPathArityException() {

    }

    public XPathArityException(String s, int required, int actual) {
        super("cannot handle " + s + " had " + actual + " arguments but required " + required + ".");
    }
}
