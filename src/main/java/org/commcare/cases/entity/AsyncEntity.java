package org.commcare.cases.entity;


import static org.commcare.cases.entity.EntityStorageCache.ValueType.TYPE_NORMAL_FIELD;
import static org.commcare.cases.entity.EntityStorageCache.ValueType.TYPE_SORT_FIELD;

import org.commcare.cases.util.StringUtils;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.DetailGroup;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.Logger;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.annotation.Nullable;

/**
 * An AsyncEntity is an entity reference which is capable of building its
 * values (evaluating all Text elements/background data elements) lazily
 * rather than upfront when the entity is constructed.
 *
 * It is threadsafe.
 *
 * It will attempt to Cache its values persistently by a derived entity key rather
 * than evaluating them each time when possible. This can be slow to perform across
 * all entities internally due to the overhead of establishing the db connection, it
 * is recommended that the entities be primed externally with a bulk query.
 *
 * @author ctsims
 */
public class AsyncEntity extends Entity<TreeReference> {

    private final DetailField[] fields;
    private final Object[] data;
    private final String[] sortData;
    private final Boolean[] relevancyData;
    private final String[][] sortDataPieces;

    private final String[] altTextData;
    private final EvaluationContext context;
    private final Hashtable<String, XPathExpression> mVariableDeclarations;
    private final DetailGroup mDetailGroup;
    private final boolean cacheEnabled;
    private boolean mVariableContextLoaded = false;
    private final String mCacheIndex;
    private final String mDetailId;

    @Nullable
    private final EntityStorageCache mEntityStorageCache;

    /*
     * the Object's lock for the integrity of this object
     */
    private final Object mAsyncLock = new Object();

    public AsyncEntity(Detail detail, EvaluationContext ec,
            TreeReference t, Hashtable<String, XPathExpression> variables,
            @Nullable EntityStorageCache cache, String cacheIndex, String extraKey) {
        super(t, extraKey);
        this.fields = detail.getFields();
        this.data = new Object[fields.length];
        this.sortData = new String[fields.length];
        this.sortDataPieces = new String[fields.length][];
        this.relevancyData = new Boolean[fields.length];
        this.altTextData = new String[fields.length];
        this.context = ec;
        this.mVariableDeclarations = variables;
        this.mEntityStorageCache = cache;

        //TODO: It's weird that we pass this in, kind of, but the thing is that we don't want to figure out
        //if this ref is _cachable_ every time, since it's a pretty big lift
        this.mCacheIndex = cacheIndex;

        this.mDetailId = detail.getId();
        this.mDetailGroup = detail.getGroup();
        this.cacheEnabled = detail.isCacheEnabled();
    }

    private void loadVariableContext() {
        synchronized (mAsyncLock) {
            if (!mVariableContextLoaded) {
                //These are actually in an ordered hashtable, so we can't just get the keyset, since it's
                //in a 1.3 hashtable equivalent
                for (Enumeration<String> en = mVariableDeclarations.keys(); en.hasMoreElements(); ) {
                    String key = en.nextElement();
                    context.setVariable(key, FunctionUtils.unpack(mVariableDeclarations.get(key).eval(context)));
                }
                mVariableContextLoaded = true;
            }
        }
    }

    @Override
    public Object getField(int i) {
        synchronized (mAsyncLock) {
            if (data[i] != null) {
                return data[i];
            }
            if (!fields[i].isCacheEnabled()) {
                data[i] = evaluateField(i);
                return data[i];
            }
            String cacheKey = null;
            if (data[i] == null) {
                if (mEntityStorageCache != null && mCacheIndex != null) {
                    cacheKey = mEntityStorageCache.getCacheKey(mDetailId, String.valueOf(i),
                            TYPE_NORMAL_FIELD);
                    // Return from the cache if we have a value
                    String value = mEntityStorageCache.retrieveCacheValue(mCacheIndex, cacheKey);
                    if (value != null) {
                        data[i] = value;
                        return data[i];
                    }
                }
            }
            data[i] = evaluateField(i);
            if (mEntityStorageCache != null && mCacheIndex != null) {
                mEntityStorageCache.cache(mCacheIndex, cacheKey, String.valueOf(data[i]));
            }
        }
        return data[i];
    }

    private Object evaluateField(int i) {
        loadVariableContext();
        try {
            data[i] = fields[i].getTemplate().evaluate(context);
        } catch (XPathException xpe) {
            Logger.exception("Error while evaluating field for case list ", xpe);
            data[i] = "<invalid xpath: " + xpe.getMessage() + ">";
        }
        return data[i];
    }

    @Override
    public String getNormalizedField(int i) {
        String normalized = this.getSortField(i);
        if (normalized == null) {
            return "";
        }
        return normalized;
    }

    @Override
    public String getSortField(int i) {
        synchronized (mAsyncLock) {
            if (sortData[i] != null) {
                return sortData[i];
            }

            // eval and return if field is not marked as optimize
            if (cacheEnabled && !fields[i].isCacheEnabled()) {
                evaluateSortData(i);
                return sortData[i];
            }

            String cacheKey = null;
            if (sortData[i] == null) {
                Text sortText = fields[i].getSort();
                if (sortText == null) {
                    return null;
                }

                if (mEntityStorageCache != null) {
                    if (cacheEnabled) {
                        cacheKey = mEntityStorageCache.getCacheKey(mDetailId, String.valueOf(i),
                                TYPE_SORT_FIELD);
                    } else {
                        // old cache and index
                        cacheKey = i + "_" + TYPE_SORT_FIELD;
                    }
                    if (mCacheIndex != null) {
                        //Check the cache!
                        String value = mEntityStorageCache.retrieveCacheValue(mCacheIndex, cacheKey);
                        if (value != null) {
                            this.setSortData(i, value);
                            return sortData[i];
                        }

                    }
                }
            }
            evaluateSortData(i);
            mEntityStorageCache.cache(mCacheIndex, cacheKey, sortData[i]);
            return sortData[i];
        }
    }

    private void evaluateSortData(int i) {
        loadVariableContext();
        try {
            Text sortText = fields[i].getSort();
            if (sortText == null) {
                this.setSortData(i, getFieldString(i));
            } else {
                this.setSortData(i, StringUtils.normalize(sortText.evaluate(context)));
            }
        } catch (XPathException xpe) {
            Logger.exception("Error while evaluating sort field", xpe);
            sortData[i] = "<invalid xpath: " + xpe.getMessage() + ">";
        }
    }

    @Override
    public int getNumFields() {
        return fields.length;
    }

    @Override
    public boolean isValidField(int fieldIndex) {
        //NOTE: This totally jacks the asynchronicity. It's only used in
        //detail fields for now, so not super important, but worth bearing
        //in mind
        synchronized (mAsyncLock) {
            loadVariableContext();
            if (getField(fieldIndex).equals("")) {
                return false;
            }
            return getRelevancyData(fieldIndex);
        }
    }

    private boolean getRelevancyData(int i) {
        synchronized (mAsyncLock) {
            if (relevancyData[i] != null) {
                return relevancyData[i];
            }
            loadVariableContext();
            try {
                relevancyData[i] = fields[i].isRelevant(context);
            } catch (XPathSyntaxException e) {
                final String msg = "Invalid relevant condition for field : " + fields[i].getHeader().toString();
                Logger.exception(msg, e);
                throw new XPathException(e);
            }
            return relevancyData[i];
        }
    }

    @Override
    public Object[] getData() {
        for (int i = 0; i < this.getNumFields(); ++i) {
            this.getField(i);
        }
        return data;
    }

    @Override
    public String[] getSortFieldPieces(int i) {
        if (getSortField(i) == null) {
            return new String[0];
        }
        return sortDataPieces[i];
    }

    private void setSortData(int i, String val) {
        synchronized (mAsyncLock) {
            this.sortData[i] = val;
            this.sortDataPieces[i] = breakUpField(val);
        }
    }

    private void setFieldData(int i, String val) {
        synchronized (mAsyncLock) {
            data[i] = val;
        }
    }

    public void setSortData(String cacheKey, String val) {
        if (mEntityStorageCache == null) {
            throw new IllegalStateException("No entity cache defined");
        }
        int fieldIndex = mEntityStorageCache.getFieldIdFromCacheKey(mDetailId, cacheKey);
        if (fieldIndex != -1) {
            setSortData(fieldIndex, val);
        }
    }

    public void setFieldData(String cacheKey, String val) {
        if (mEntityStorageCache == null) {
            throw new IllegalStateException("No entity cache defined");
        }
        int fieldIndex = mEntityStorageCache.getFieldIdFromCacheKey(mDetailId, cacheKey);
        if (fieldIndex != -1) {
            setFieldData(fieldIndex, val);
        }
    }

    private static String[] breakUpField(String input) {
        if (input == null) {
            return new String[0];
        } else {
            //We always fuzzy match on the sort field and only if it is available
            //(as a way to restrict possible matching)
            return input.split("\\s+");
        }
    }

    @Nullable
    @Override
    public String getGroupKey() {
        if (mDetailGroup != null) {
            return (String)mDetailGroup.getFunction().eval(context);
        }
        return null;
    }

    @Nullable
    public String getAltTextData(int i) {
        synchronized (mAsyncLock) {
            if (altTextData[i] != null) {
                return altTextData[i];
            }
            loadVariableContext();
            Text altText = fields[i].getAltText();
            if (altText != null) {
                try {
                    altTextData[i] = altText.evaluate(context);
                } catch (XPathException xpe) {
                    Logger.exception("Error while evaluating field for case list ", xpe);
                    altTextData[i] = "<invalid xpath: " + xpe.getMessage() + ">";
                }
            }
            return altTextData[i];
        }
    }

    @Override
    public String[] getAltText() {
        for (int i = 0; i < this.getNumFields(); ++i) {
            this.getAltTextData(i);
        }
        return altTextData;
    }
}
