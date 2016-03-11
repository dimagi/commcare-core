package org.javarosa.xpath;

public class XPathException extends RuntimeException {

    //A reference to the "Source" of this message helpful
    //for tracking down where the invalid xpath was declared
    String sourceRef;

    public XPathException() {

    }

    public XPathException(String s) {
        super("XPath evaluation: " + s);
    }

    public void setSource(String source) {
        this.sourceRef = source;
    }

    public String getSource() {
        return sourceRef;
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#getMessage()
     */
    public String getMessage() {
        if (sourceRef == null) {
            return super.getMessage();
        } else {
            return "The problem was located in " + sourceRef + "\n" + super.getMessage();
        }
    }
}
