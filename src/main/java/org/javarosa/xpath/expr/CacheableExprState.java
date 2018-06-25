package org.javarosa.xpath.expr;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Holder object for all of the state values an InFormCacheableExpr needs to keep track of
 */
public class CacheableExprState implements Externalizable {

    protected boolean computedCacheability;
    protected boolean exprIsCacheable;
    protected boolean computedContextTypes;
    protected boolean contextRefIsRelevant;
    protected boolean originalContextRefIsRelevant;

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        computedCacheability = ExtUtil.readBool(in);
        exprIsCacheable = ExtUtil.readBool(in);
        computedContextTypes = ExtUtil.readBool(in);
        contextRefIsRelevant = ExtUtil.readBool(in);
        originalContextRefIsRelevant = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeBool(out, computedCacheability);
        ExtUtil.writeBool(out, exprIsCacheable);
        ExtUtil.writeBool(out, computedContextTypes);
        ExtUtil.writeBool(out, contextRefIsRelevant);
        ExtUtil.writeBool(out, originalContextRefIsRelevant);
    }
}
