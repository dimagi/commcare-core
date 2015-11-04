package org.commcare.api.json;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.form.api.FormEntryPrompt;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by willpride on 11/3/15.
 */
public class PromptToJson {
    public static JSONObject formEntryPromptToJson(FormEntryPrompt prompt) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("caption_audio", prompt.getAudioText());
        obj.put("caption_image", prompt.getImageText());
        obj.put("caption_video", prompt.getVideoText());
        obj.put("caption_markdown", prompt.getMarkdownText());
        obj.put("help", prompt.getHelpText());
        obj.put("binding", prompt.getQuestion().getBind().getReference().toString());
        obj.put("style", parseStyle(prompt));
        obj.put("datatype", parseControlType(prompt));
        obj.put("required", prompt.isRequired());
        obj.put("answer", prompt.getAnswerValue());

        if(prompt.getDataType() == Constants.DATATYPE_CHOICE || prompt.getDataType() == Constants.DATATYPE_CHOICE_LIST){
            obj.put("choices", parseSelect(prompt));
        }
        return obj;
    }

    private static JSONArray parseSelect(FormEntryPrompt prompt) {
        JSONArray obj = new JSONArray();
        for(SelectChoice choice: prompt.getSelectChoices()){
            obj.put(choice.getLabelInnerText());
        }
        return obj;
    }

    //TODO WSP: What the fuck is drew doing XFormPlayer parse_style_info
    private static String parseStyle(FormEntryPrompt prompt) {
        return prompt.getAppearanceHint();
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
