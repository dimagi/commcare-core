/**
 *
 */
package org.javarosa.core.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

/**
 * A Submission Profile is a class which is responsible for
 * holding and processing the details of how a submission
 * should be handled.
 *
 * @author ctsims
 */
public class SubmissionProfile implements Externalizable {

    XPathReference ref;
    String method;
    String action;
    String mediaType;
    Hashtable<String, String> attributeMap;

    public SubmissionProfile() {

    }

    public SubmissionProfile(XPathReference ref, String method, String action, String mediatype) {
        this(ref, method, action, mediatype, new Hashtable<String, String>());
    }

    public SubmissionProfile(XPathReference ref, String method, String action, String mediatype, Hashtable<String, String> attributeMap) {
        this.method = method;
        this.ref = ref;
        this.action = action;
        this.mediaType = mediatype;
        this.attributeMap = attributeMap;
    }

    public XPathReference getRef() {
        return ref;
    }

    public String getMethod() {
        return method;
    }

    public String getAction() {
        return action;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getAttribute(String name) {
        return attributeMap.get(name);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        ref = (XPathReference)ExtUtil.read(in, new ExtWrapTagged(XPathReference.class));
        method = ExtUtil.readString(in);
        action = ExtUtil.readString(in);
        mediaType = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        attributeMap = (Hashtable<String, String>)ExtUtil.read(in, new ExtWrapMap(String.class, String.class));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(ref));
        ExtUtil.writeString(out, method);
        ExtUtil.writeString(out, action);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(mediaType));
        ExtUtil.write(out, new ExtWrapMap(attributeMap));
    }


}
