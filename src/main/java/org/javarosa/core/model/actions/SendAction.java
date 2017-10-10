package org.javarosa.core.model.actions;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.SubmissionProfile;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.Recalculate;
import org.javarosa.core.model.data.AnswerDataFactory;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.Logger;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xform.parse.IElementHandler;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.kxml2.kdom.Element;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * A Send Action is responsible for loading a submission template from the form, and performing
 * a callout action in the current platform to retrieve a value synchronously from a web
 * service.
 *
 * @author ctsims
 */
public class SendAction extends Action {

    private String submissionId;

    public static final String ELEMENT_NAME = "send";

    public SendAction() {
        // for externalization
    }

    public SendAction(String submissionId) {
        super(ELEMENT_NAME);
        this.submissionId = submissionId;
    }


    public static IElementHandler getHandler() {
        return new IElementHandler() {
            @Override
            public void handle(XFormParser p, Element e, Object parent) {
                // the generic parseAction() method in XFormParser already checks to make sure
                // that parent is an IFormElement, and throws an exception if it is not
                p.parseSendAction(((IFormElement) parent).getActionController(), e);
            }
        };
    }

    @Override
    public TreeReference processAction(FormDef model, TreeReference contextRef) {
        SubmissionProfile profile = model.getSubmissionProfile(this.submissionId);
        String url = profile.getResource();

        TreeReference ref = profile.getRef();
        Map<String, String> map = null;
        if(ref != null) {
            map = getKeyValueMapping(model, ref);
        }

        String result = null;
        try {
            result = model.dispatchSendCallout(url, map);
        } catch (Exception e ) {
            Logger.exception("send-action", e);
        }
        if(result == null) {
            return null;
        } else {
            TreeReference target = profile.getTargetRef();
            model.setValue(new UncastData(result), target);
            return target;
        }
    }

    private Map<String, String> getKeyValueMapping(FormDef model, TreeReference ref) {
        Map<String, String> map = new HashMap<>();
        AbstractTreeElement element = model.getEvaluationContext().resolveReference(ref);
        for(int i = 0 ; i < element.getNumChildren() ; ++i) {
            AbstractTreeElement child = element.getChildAt(i);

            String name = child.getName();
            IAnswerData value = child.getValue();

            if(value != null) {
                map.put(name, value.uncast().getString());
            }
        }
        return map;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        submissionId = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, submissionId);
    }
}
