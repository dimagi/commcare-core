package org.commcare.suite.model;

import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapMapPoly;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

/**
 * Entry config for querying a remote server with user and session provided
 * parameters and storing the xml data response in an instance.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class RemoteQueryDatum extends SessionDatum {
    private Hashtable<String, XPathExpression> hiddenQueryValues;
    private OrderedHashtable<String, QueryPrompt> userQueryPrompts;
    private boolean useCaseTemplate;
    private boolean defaultSearch;
    private Text title;

    @SuppressWarnings("unused")
    public RemoteQueryDatum() {
    }

    /**
     * @param useCaseTemplate True if query results respect the casedb
     *                        template structure. Permits flexibility (path
     *                        heterogeneity) in case data lookups
     */
    public RemoteQueryDatum(URL url, String storageInstance,
                            Hashtable<String, XPathExpression> hiddenQueryValues,
                            OrderedHashtable<String, QueryPrompt> userQueryPrompts,
                            boolean useCaseTemplate, boolean defaultSearch, Text title) {
        super(storageInstance, url.toString());
        this.hiddenQueryValues = hiddenQueryValues;
        this.userQueryPrompts = userQueryPrompts;
        this.useCaseTemplate = useCaseTemplate;
        this.defaultSearch = defaultSearch;
        this.title = title;
    }

    public OrderedHashtable<String, QueryPrompt> getUserQueryPrompts() {
        return userQueryPrompts;
    }

    public Hashtable<String, XPathExpression> getHiddenQueryValues() {
        return hiddenQueryValues;
    }

    public Text getTitleText() {
        return title;
    }

    public URL getUrl() {
        try {
            return new URL(getValue());
        } catch (MalformedURLException e) {
            // Not possible given constructor passes in a valid URL
            e.printStackTrace();
            return null;
        }
    }

    public boolean useCaseTemplate() {
        return useCaseTemplate;
    }

    public boolean doDefaultSearch() {
        return defaultSearch;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);

        hiddenQueryValues =
                (Hashtable<String, XPathExpression>)ExtUtil.read(in, new ExtWrapMapPoly(String.class), pf);
        userQueryPrompts =
                (OrderedHashtable<String, QueryPrompt>)ExtUtil.read(in,
                        new ExtWrapMap(String.class, QueryPrompt.class, ExtWrapMap.TYPE_ORDERED), pf);
        title = (Text) ExtUtil.read(in, new ExtWrapNullable(Text.class), pf);
        useCaseTemplate = ExtUtil.readBool(in);
        defaultSearch = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);

        ExtUtil.write(out, new ExtWrapMapPoly(hiddenQueryValues));
        ExtUtil.write(out, new ExtWrapMap(userQueryPrompts));
        ExtUtil.write(out, new ExtWrapNullable(title));
        ExtUtil.writeBool(out, useCaseTemplate);
        ExtUtil.writeBool(out, defaultSearch);
    }
}
