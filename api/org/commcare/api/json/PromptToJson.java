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
import org.javarosa.form.api.FormEntryCaption;
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

    public static String parseEvent(FormEntryModel model){
        JSONObject obj = new JSONObject();
        parseQuestionType(model, obj);
        parseQuestion(model, model.getQuestionPrompt(), obj);
        return obj.toString();
    }

    public static String formEntryModelToJson(FormEntryModel model) throws JSONException {
        return formEntryModelToJson(model, model.getQuestionPrompt());
    }

    public static String formEntryModelToJson(FormEntryModel model, FormEntryPrompt prompt) throws JSONException {
        JSONObject obj = new JSONObject();
        parseQuestion(model, prompt, obj);
        return obj.toString();
    }

    public static void parseCaption(FormEntryCaption prompt, JSONObject obj){
        obj.put("caption_audio", jsonNullIfNull(prompt.getAudioText()));
        obj.put("caption", jsonNullIfNull(prompt.getLongText()));
        obj.put("caption_image", jsonNullIfNull(prompt.getImageText()));
        obj.put("caption_video", jsonNullIfNull(prompt.getVideoText()));
        obj.put("caption_markdown", jsonNullIfNull(prompt.getMarkdownText()));
    }

    public static void parseQuestion(FormEntryModel model, FormEntryPrompt prompt, JSONObject obj){
        parseCaption(prompt, obj);
        obj.put("help", jsonNullIfNull(prompt.getHelpText()));
        obj.put("binding", jsonNullIfNull(prompt.getQuestion().getBind().getReference().toString()));
        obj.put("style", jsonNullIfNull(parseStyle(prompt)));
        obj.put("datatype", jsonNullIfNull(parseControlType(prompt)));
        obj.put("required", jsonNullIfNull(prompt.isRequired()));
        try {
            parsePutAnswer(obj, prompt);
        } catch(Exception e){
            e.printStackTrace();
        }
        obj.put("ix", jsonNullIfNull(prompt.getIndex()));

        if(prompt.getDataType() == Constants.DATATYPE_CHOICE || prompt.getDataType() == Constants.DATATYPE_CHOICE_LIST){
            obj.put("choices", parseSelect(prompt));
        }
    }

    public static Object jsonNullIfNull(Object obj){
        return obj == null ? JSONObject.NULL : obj;
    }

    public static void parseQuestionType(FormEntryModel model, JSONObject obj) {
        int status = model.getEvent();
        obj.put("ix", model.getFormIndex().toString());
        switch(status) {
            case FormEntryController.EVENT_BEGINNING_OF_FORM:
                obj.put("type", "form-start");
                return;
            case FormEntryController.EVENT_END_OF_FORM:
                obj.put("type", "form-entry-complete");
                return;
            case FormEntryController.EVENT_QUESTION:
                obj.put("type", "question");
                parseQuestion(model, model.getQuestionPrompt(), obj);
                return;
            case FormEntryController.EVENT_REPEAT_JUNCTURE:
                obj.put("type", "repeat-juncture");
                return;
                //parse repeat
            case FormEntryController.EVENT_GROUP:
                // we're in a subgroup
                parseCaption(model.getCaptionPrompt(), obj);
                obj.put("type", "sub-group");
                obj.put("repeatable", false);
                break;
            case FormEntryController.EVENT_REPEAT:
                // we're in a subgroup
                parseCaption(model.getCaptionPrompt(), obj);
                obj.put("type", "sub-group");
                obj.put("repeatable", true);
                obj.put("exists", true);
                break;
            case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                // we're in a subgroup
                parseCaption(model.getCaptionPrompt(), obj);
                obj.put("type", "sub-group");
                obj.put("repeatable", true);
                obj.put("exists", false);
                break;
        }
    }

    private static void parsePutAnswer(JSONObject obj, FormEntryPrompt prompt){
        IAnswerData answerValue = prompt.getAnswerValue();
        if (answerValue == null){
            obj.put("answer", JSONObject.NULL);
            return;
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
                obj.put("answer", ((Date)answerValue.getValue()).getTime()/1000);
                return;
            case Constants.DATATYPE_TIME:
                obj.put("answer", answerValue.getDisplayText());
                return;
            case Constants.DATATYPE_DATE_TIME:
                obj.put("answer", ((Date)answerValue.getValue()).getTime()/1000);
                return;
            case Constants.DATATYPE_CHOICE:
                obj.put("answer", ((Selection)answerValue.getValue()).index + 1);
                return;
            case Constants.DATATYPE_CHOICE_LIST:
                Vector<Selection> selections = ((SelectMultiData)answerValue).getValue();
                JSONArray acc = new JSONArray();
                for(Selection selection: selections){
                    int ordinal = selection.index + 1;
                    acc.put(ordinal);
                }
                obj.put("answer", acc);
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
