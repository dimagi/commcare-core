package org.javarosa.model.xform;

import org.javarosa.core.api.IModule;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.data.AnswerDataFactory;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.util.restorable.IXFormyFactory;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.transport.payload.IDataPayload;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.XFormAnswerDataSerializer;
import org.javarosa.xpath.XPathConditional;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.io.IOException;

public class XFormsModule implements IModule {

    public void registerModule() {
        String[] classes = {
                "org.javarosa.model.xform.XPathReference",
                "org.javarosa.xpath.XPathConditional"
        };

        PrototypeManager.registerPrototypes(classes);
        PrototypeManager.registerPrototypes(XPathParseTool.xpathClasses);
        RestoreUtils.xfFact = new IXFormyFactory() {
            public TreeReference ref(String refStr) {
                return FormInstance.unpackReference(new XPathReference(refStr));
            }

            public IDataPayload serializeInstance(FormInstance dm) {
                try {
                    return (new XFormSerializingVisitor()).createSerializedPayload(dm);
                } catch (IOException e) {
                    return null;
                }
            }

            public IAnswerData parseData(String textVal, int dataType, TreeReference ref, FormDef f) {
                return AnswerDataFactory.templateByDataType(dataType).cast(new UncastData(textVal));
            }

            public String serializeData(IAnswerData data) {
                return (String)(new XFormAnswerDataSerializer().serializeAnswerData(data));
            }

            public IConditionExpr refToPathExpr(TreeReference ref) {
                return new XPathConditional(XPathPathExpr.fromRef(ref));
            }
        };
    }

}
