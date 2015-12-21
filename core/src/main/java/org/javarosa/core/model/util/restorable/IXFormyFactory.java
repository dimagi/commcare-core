package org.javarosa.core.model.util.restorable;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.transport.payload.IDataPayload;

public interface IXFormyFactory {
    TreeReference ref(String refStr);

    IDataPayload serializeInstance(FormInstance dm);

    IAnswerData parseData(String textVal, int dataType, TreeReference ref, FormDef f);

    String serializeData(IAnswerData data);

    //kinda ghetto
    IConditionExpr refToPathExpr(TreeReference ref);
}
