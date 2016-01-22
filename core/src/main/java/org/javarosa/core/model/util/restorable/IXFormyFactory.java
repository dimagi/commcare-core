package org.javarosa.core.model.util.restorable;

import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.transport.payload.IDataPayload;

public interface IXFormyFactory {
    TreeReference ref(String refStr);

    IDataPayload serializeInstance(FormInstance dm);

    //kinda ghetto
    IConditionExpr refToPathExpr(TreeReference ref);
}
