package org.commcare.api.json;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.form.api.FormEntryPrompt;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Vector;

/**
 * Created by willpride on 11/3/15.
 */
public class PromptToJson {
    public static JSONObject formEntryModelToJson(FormEntryModel model) throws JSONException {
        FormEntryPrompt prompt = model.getQuestionPrompt();
        JSONObject obj = new JSONObject();
        obj.put("caption_audio", jsonNullIfNull(prompt.getAudioText()));
        obj.put("caption", jsonNullIfNull(prompt.getLongText()));
        obj.put("caption_image", jsonNullIfNull(prompt.getImageText()));
        obj.put("caption_video", jsonNullIfNull(prompt.getVideoText()));
        obj.put("caption_markdown", jsonNullIfNull(prompt.getMarkdownText()));
        obj.put("help", jsonNullIfNull(prompt.getHelpText()));
        obj.put("binding", jsonNullIfNull(prompt.getQuestion().getBind().getReference().toString()));
        obj.put("style", jsonNullIfNull(parseStyle(prompt)));
        obj.put("datatype", jsonNullIfNull(parseControlType(prompt)));
        obj.put("required", jsonNullIfNull(prompt.isRequired()));
        parsePutAnswer(obj, prompt);
        obj.put("ix", jsonNullIfNull(prompt.getIndex()));
        parseQuestionType(model, obj);

        if(prompt.getDataType() == Constants.DATATYPE_CHOICE || prompt.getDataType() == Constants.DATATYPE_CHOICE_LIST){
            obj.put("choices", parseSelect(prompt));
        }
        return obj;
    }

    public static Object jsonNullIfNull(Object obj){
        return obj == null ? JSONObject.NULL : obj;
    }

    private static void parseQuestionType(FormEntryModel model, JSONObject obj) {
        int status = model.getEvent();
        switch(status) {
            case FormEntryController.EVENT_BEGINNING_OF_FORM:
                obj.put("type", "form-start");
                return;
            case FormEntryController.EVENT_END_OF_FORM:
                obj.put("type", "form-entry-complete");
                return;
            case FormEntryController.EVENT_QUESTION:
                obj.put("type", "question");
                return;
                //parse question
            case FormEntryController.EVENT_REPEAT_JUNCTURE:
                obj.put("type", "repeat-juncture");
                return;
                //parse repeat
            case FormEntryController.EVENT_GROUP:
                // we're in a subgroup
                obj.put("type", "subgroup");
                obj.put("repeatable", false);
                break;
            case FormEntryController.EVENT_REPEAT:
                // we're in a subgroup
                obj.put("type", "subgroup");
                obj.put("repeatable", true);
                obj.put("exists", true);
                break;
            case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                // we're in a subgroup
                obj.put("type", "subgroup");
                obj.put("repeatable", true);
                obj.put("exists", false);
                break;
        }
    }

    private static void parsePutAnswer(JSONObject obj, FormEntryPrompt prompt){
        IAnswerData answerValue = prompt.getAnswerValue();
        if (answerValue == null){
            obj.put("answer", JSONObject.NULL);
        }
        switch(prompt.getDataType()) {
            case Constants.DATATYPE_NULL:
            case Constants.DATATYPE_TEXT:
                obj.put("answer", answerValue.getDisplayText());
                return;
            case Constants.DATATYPE_INTEGER:
                obj.put("answer", (int)answerValue.getValue());
                return;
            case Constants.DATATYPE_LONG:
            case Constants.DATATYPE_DECIMAL:
                obj.put("answer", (double)answerValue.getValue());
                return;
            case Constants.DATATYPE_DATE:
                obj.put("answer", new DateData((Date) answerValue).getDisplayText());
                return;
            case Constants.DATATYPE_TIME:
                obj.put("answer", new TimeData((Date)answerValue).getDisplayText());
                return;
            case Constants.DATATYPE_DATE_TIME:
                obj.put("answer", new DateTimeData((Date)answerValue).getDisplayText());
                return;
            case Constants.DATATYPE_CHOICE:
                obj.put("answer", new SelectOneData((Selection) answerValue).getDisplayText());
                return;
            case Constants.DATATYPE_CHOICE_LIST:
                obj.put("answer", new SelectMultiData((Vector) answerValue).getDisplayText());
                return;

            /* as yet unimplemented
            case Constants.DATATYPE_GEOPOINT:
                return "geo";
            case Constants.DATATYPE_BARCODE:
                return "barcode";
            case Constants.DATATYPE_BINARY:
                return "binary"
            */

        }
    }

    private static JSONArray parseSelect(FormEntryPrompt prompt) {
        JSONArray obj = new JSONArray();
        for(SelectChoice choice: prompt.getSelectChoices()){
            obj.put(prompt.getSelectChoiceText(choice));
        }
        return obj;
    }

    //TODO WSP: What the fuck is drew doing XFormPlayer parse_style_info
    private static JSONObject parseStyle(FormEntryPrompt prompt) {
        String hint = prompt.getAppearanceHint();
        if(hint == null){
            return null;
        }
        JSONObject ret = new JSONObject().put("raw", hint);
        return ret;
    }



    private static String parseControlType(FormEntryPrompt prompt){
        if(prompt.getControlType() == Constants.CONTROL_TRIGGER){
            return "info";
        }
        switch(prompt.getDataType()){
            case Constants.DATATYPE_NULL:
            case Constants.DATATYPE_TEXT:
                return "str";
            case Constants.DATATYPE_INTEGER:
                return "int";
            case Constants.DATATYPE_LONG:
                return "longint";
            case Constants.DATATYPE_DECIMAL:
                return "float";
            case Constants.DATATYPE_DATE:
                return "date";
            case Constants.DATATYPE_TIME:
                return "time";
            case Constants.DATATYPE_CHOICE:
                return "select";
            case Constants.DATATYPE_CHOICE_LIST:
                return "multiselect";
            case Constants.DATATYPE_GEOPOINT:
                return "geo";
            case Constants.DATATYPE_DATE_TIME:
                return "datetime";
            case Constants.DATATYPE_BARCODE:
                return "barcode";
            case Constants.DATATYPE_BINARY:
                return "binary";
        }
        return "unrecognized";
    }
}
