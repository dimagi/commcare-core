package org.commcare.suite.model;

import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Entry config for querying a remote server with user and session provided
 * parameters and storing the xml data response in an instance.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class RemoteQueryDatum extends SessionDatum {
    private List<QueryData> hiddenQueryValues;
    private OrderedHashtable<String, QueryPrompt> userQueryPrompts;
    private boolean useCaseTemplate;
    private boolean defaultSearch;
    private String titleLocaleId;

    @SuppressWarnings("unused")
    public RemoteQueryDatum() {
    }

    /**
     * @param useCaseTemplate True if query results respect the casedb
     *                        template structure. Permits flexibility (path
     *                        heterogeneity) in case data lookups
     */
    public RemoteQueryDatum(URL url, String storageInstance,
            List<QueryData> hiddenQueryValues,
                            OrderedHashtable<String, QueryPrompt> userQueryPrompts,
                            boolean useCaseTemplate, boolean defaultSearch, String titleLocaleId) {
        super(storageInstance, url.toString());
        this.hiddenQueryValues = hiddenQueryValues;
        this.userQueryPrompts = userQueryPrompts;
        this.useCaseTemplate = useCaseTemplate;
        this.defaultSearch = defaultSearch;
        this.titleLocaleId = titleLocaleId;
    }

    public OrderedHashtable<String, QueryPrompt> getUserQueryPrompts() {
        return userQueryPrompts;
    }

    public List<QueryData> getHiddenQueryValues() {
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

    public String getTitleLocaleId() {
        return titleLocaleId;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);

        hiddenQueryValues =
                (List<QueryData>) ExtUtil.read(in, new ExtWrapList(new ExtWrapTagged()), pf);
        userQueryPrompts =
                (OrderedHashtable<String, QueryPrompt>)ExtUtil.read(in,
                        new ExtWrapMap(String.class, QueryPrompt.class, ExtWrapMap.TYPE_ORDERED), pf);
        useCaseTemplate = ExtUtil.readBool(in);
        defaultSearch = ExtUtil.readBool(in);
        titleLocaleId = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.write(out, new ExtWrapList(hiddenQueryValues, new ExtWrapTagged()));
        ExtUtil.write(out, new ExtWrapMap(userQueryPrompts));
        ExtUtil.writeBool(out, useCaseTemplate);
        ExtUtil.writeBool(out, defaultSearch);
        ExtUtil.writeString(out, titleLocaleId);
    }
}
