package org.commcare.suite.model;

import com.google.common.collect.Multimap;

import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapArrayListMultiMap;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Entry config for querying a remote server with user and session provided
 * parameters and storing the xml data response in an instance.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class RemoteQueryDatum extends SessionDatum {
    private Multimap<String, XPathExpression> hiddenQueryValues;
    private OrderedHashtable<String, QueryPrompt> userQueryPrompts;
    private boolean useCaseTemplate;
    private boolean defaultSearch;

    @SuppressWarnings("unused")
    public RemoteQueryDatum() {
    }

    /**
     * @param useCaseTemplate True if query results respect the casedb
     *                        template structure. Permits flexibility (path
     *                        heterogeneity) in case data lookups
     */
    public RemoteQueryDatum(URL url, String storageInstance,
                            Multimap<String, XPathExpression> hiddenQueryValues,
                            OrderedHashtable<String, QueryPrompt> userQueryPrompts,
                            boolean useCaseTemplate, boolean defaultSearch) {
        super(storageInstance, url.toString());
        this.hiddenQueryValues = hiddenQueryValues;
        this.userQueryPrompts = userQueryPrompts;
        this.useCaseTemplate = useCaseTemplate;
        this.defaultSearch = defaultSearch;
    }

    public OrderedHashtable<String, QueryPrompt> getUserQueryPrompts() {
        return userQueryPrompts;
    }

    public Multimap<String, XPathExpression> getHiddenQueryValues() {
        return hiddenQueryValues;
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
                (Multimap<String, XPathExpression>)ExtUtil.read(in, new ExtWrapArrayListMultiMap(String.class), pf);
        userQueryPrompts =
                (OrderedHashtable<String, QueryPrompt>)ExtUtil.read(in,
                        new ExtWrapMap(String.class, QueryPrompt.class, ExtWrapMap.TYPE_ORDERED), pf);
        useCaseTemplate = ExtUtil.readBool(in);
        defaultSearch = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.write(out, new ExtWrapArrayListMultiMap(hiddenQueryValues));
        ExtUtil.write(out, new ExtWrapMap(userQueryPrompts));
        ExtUtil.writeBool(out, useCaseTemplate);
        ExtUtil.writeBool(out, defaultSearch);
    }
}
