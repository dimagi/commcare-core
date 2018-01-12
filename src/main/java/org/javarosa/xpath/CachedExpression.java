package org.javarosa.xpath;

import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Created by amstone326 on 1/11/18.
 */

public class CachedExpression implements Persistable {

    public static final String STORAGE_KEY = "cachedexpressions";

    private Object evalResult;
    private int recordId = -1;

    public CachedExpression() {

    }

    public CachedExpression(InFormCacheableExpr expression, Object evalResult) {
        this.evalResult = evalResult;
    }

    public Object getEvalResult() {
        return this.evalResult;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        try {
            Class objectType = Class.forName(ExtUtil.readString(in));
            this.evalResult = ExtUtil.read(in, objectType, pf);
        } catch (ClassNotFoundException e) {
            // ??
        }
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, evalResult.getClass().getName());

        if (evalResult instanceof Boolean) {
            ExtUtil.writeBool(out, (Boolean)evalResult);
        } else if (evalResult instanceof Double) {
            ExtUtil.writeDecimal(out, (Double)evalResult);
        } else if (evalResult instanceof String) {
            ExtUtil.writeString(out, (String)evalResult);
        } else if (evalResult instanceof Date) {
            ExtUtil.writeDate(out, (Date)evalResult);
        } else {
            throw new XPathTypeMismatchException("Unexpected object type in CachedExpressionValue: " + evalResult);
        }
    }

    @Override
    public void setID(int ID) {
        recordId = ID;
    }

    @Override
    public int getID() {
        return recordId;
    }
}
