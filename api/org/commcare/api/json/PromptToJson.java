package org.commcare.api.json;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.*;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.form.api.FormEntryPrompt;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

/**
 * Functions for generating the JSON repesentation of a FormEntryPrompt
 *
 * @author wspride
 */
public class PromptToJson {

    /**
     * @param prompt       The FormEntryPrompt under consideration
     * @param questionJson the JSON object question representation being generated
     */
    public static void parseQuestion(FormEntryPrompt prompt, JSONObject questionJson) {
        parseCaption(prompt, questionJson);
        questionJson.put("help", jsonNullIfNull(prompt.getHelpText()));
        questionJson.put("binding", jsonNullIfNull(prompt.getQuestion().getBind().getReference().toString()));
        questionJson.put("style", jsonNullIfNull(parseStyle(prompt)));
        questionJson.put("datatype", jsonNullIfNull(parseControlType(prompt)));
        questionJson.put("required", prompt.isRequired() ? 1 : 0);
        try {
            parseQuestionAnswer(questionJson, prompt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        questionJson.put("ix", jsonNullIfNull(prompt.getIndex()));

        if (prompt.getDataType() == Constants.DATATYPE_CHOICE || prompt.getDataType() == Constants.DATATYPE_CHOICE_LIST) {
            questionJson.put("choices", parseSelect(prompt));
        }
    }

    /**
     * @param prompt       The FormEntryCaption to get the caption fields from
     * @param questionJson The JSON question representation being built
     */
    public static void parseCaption(FormEntryCaption prompt, JSONObject questionJson) {
        questionJson.put("caption_audio", jsonNullIfNull(prompt.getAudioText()));
        questionJson.put("caption", jsonNullIfNull(prompt.getLongText()));
        questionJson.put("caption_image", jsonNullIfNull(prompt.getImageText()));
        questionJson.put("caption_video", jsonNullIfNull(prompt.getVideoText()));
        questionJson.put("caption_markdown", jsonNullIfNull(prompt.getMarkdownText()));
    }

    // We want to use the JSONObject null if the object is null, not the Java null
    public static Object jsonNullIfNull(Object obj) {
        return obj == null ? JSONObject.NULL : obj;
    }

    public static void parseQuestionType(FormEntryModel model, JSONObject obj) {
        int status = model.getEvent();
        FormIndex ix = model.getFormIndex();
        obj.put("ix", ix.toString());

        switch (status) {
            case FormEntryController.EVENT_BEGINNING_OF_FORM:
                obj.put("type", "form-start");
                return;
            case FormEntryController.EVENT_END_OF_FORM:
                obj.put("type", "form-entry-complete");
                return;
            case FormEntryController.EVENT_QUESTION:
                obj.put("type", "question");
                parseQuestion(model.getQuestionPrompt(), obj);
                return;
            case FormEntryController.EVENT_REPEAT_JUNCTURE:
                obj.put("type", "repeat-juncture");
                parseRepeatJuncture(model, obj, ix);
                return;
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

    private static void parseRepeatJuncture(FormEntryModel model, JSONObject obj, FormIndex ix) {
        FormEntryCaption formEntryCaption = model.getCaptionPrompt(ix);
        FormEntryCaption.RepeatOptions repeatOptions = formEntryCaption.getRepeatOptions();
        parseCaption(formEntryCaption, obj);
        obj.put("header", repeatOptions.header);
        obj.put("repetitions", formEntryCaption.getRepetitionsText());
        obj.put("add-choice", repeatOptions.add);
        obj.put("delete-choice", repeatOptions.delete);
        obj.put("del-header", repeatOptions.delete_header);
        obj.put("done-choice", repeatOptions.done);
    }

    private static void parseQuestionAnswer(JSONObject obj, FormEntryPrompt prompt) {
        IAnswerData answerValue = prompt.getAnswerValue();
        if (answerValue == null) {
            obj.put("answer", JSONObject.NULL);
            return;
        }
        switch (prompt.getDataType()) {
            case Constants.DATATYPE_NULL:
            case Constants.DATATYPE_TEXT:
                obj.put("answer", answerValue.getDisplayText());
                return;
            case Constants.DATATYPE_INTEGER:
                obj.put("answer", (int) answerValue.getValue());
                return;
            case Constants.DATATYPE_LONG:
            case Constants.DATATYPE_DECIMAL:
                obj.put("answer", (double) answerValue.getValue());
                return;
            case Constants.DATATYPE_DATE:
                obj.put("answer", (DateUtils.formatDate((Date) answerValue.getValue(), DateUtils.FORMAT_ISO8601)));
                return;
            case Constants.DATATYPE_TIME:
                obj.put("answer", answerValue.getDisplayText());
                return;
            case Constants.DATATYPE_DATE_TIME:
                obj.put("answer", ((Date) answerValue.getValue()).getTime());
                return;
            case Constants.DATATYPE_CHOICE:
                Selection singleSelection = ((Selection) answerValue.getValue());
                singleSelection.attachChoice(prompt.getQuestion());
                obj.put("answer", ((Selection) answerValue.getValue()).getTouchformsIndex());
                return;
            case Constants.DATATYPE_CHOICE_LIST:
                Vector<Selection> selections = ((SelectMultiData) answerValue).getValue();
                JSONArray acc = new JSONArray();
                for (Selection selection : selections) {
                    selection.attachChoice(prompt.getQuestion());
                    int ordinal = selection.getTouchformsIndex();
                    acc.put(ordinal);
                }
                obj.put("answer", acc);
                return;
            case Constants.DATATYPE_GEOPOINT:
                GeoPointData geoPointData = ((GeoPointData) prompt.getAnswerValue());
                double[] coords = new double[]{geoPointData.getLatitude(), geoPointData.getLongitude()};
                obj.put("answer", coords);
                return;
        }
    }

    /**
     * Given a prompt, generate a JSONArray of the possible select choices. return empty array if no choices.
     */
    private static JSONArray parseSelect(FormEntryPrompt prompt) {
        JSONArray obj = new JSONArray();
        for (SelectChoice choice : prompt.getSelectChoices()) {
            obj.put(prompt.getSelectChoiceText(choice));
        }
        return obj;
    }

    //TODO WSP: What the fuck is drew doing XFormPlayer parse_style_info
    // https://github.com/dimagi/touchforms/blob/master/touchforms/backend/xformplayer.py#L400
    private static JSONObject parseStyle(FormEntryPrompt prompt) {
        String hint = prompt.getAppearanceHint();
        if (hint == null) {
            return null;
        }
        JSONObject ret = new JSONObject().put("raw", hint);
        return ret;
    }


    private static String parseControlType(FormEntryPrompt prompt) {
        if (prompt.getControlType() == Constants.CONTROL_TRIGGER) {
            return "info";
        }
        switch (prompt.getDataType()) {
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
