package org.commcare.api.json;

import org.javarosa.core.model.data.AnswerDataFactory;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.form.api.FormEntryPrompt;
import org.json.JSONObject;

/**
 * Created by willpride on 11/18/15.
 */
public class AnswerQuestionJson {
    public static JSONObject questionAnswerToJson(FormEntryController controller,
                                                  FormEntryModel model, String answer){
        JSONObject ret = new JSONObject();
        FormEntryPrompt prompt = model.getQuestionPrompt();
        IAnswerData answerData = null;
        try {
            answerData = getAnswerData(prompt, answer);
        } catch(IllegalArgumentException e){
            ret.put("status","error");
            ret.put("type", "illegal-argument");
            ret.put("reason", e.getMessage());
            return ret;
        }
        int result = controller.answerQuestion(answerData);
        if(result == FormEntryController.ANSWER_REQUIRED_BUT_EMPTY) {
            ret.put("status","error");
            ret.put("type", "required");
        }
        else if(result == FormEntryController.ANSWER_CONSTRAINT_VIOLATED){
            ret.put("status", "error");
            ret.put("type", "restraint");
            ret.put("reason", prompt.getConstraintText());
        }
        else if (result == FormEntryController.ANSWER_OK){
            ret.put("status","success");
            //controller.stepToNextEvent();
        }
        return ret;
    }

    public static IAnswerData getAnswerData(FormEntryPrompt fep, String data){
        return data.equals("") ? null : AnswerDataFactory.template(fep.getControlType(), fep.getDataType()).cast(new UncastData(data));
    }
}
