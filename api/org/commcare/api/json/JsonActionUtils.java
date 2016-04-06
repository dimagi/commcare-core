package org.commcare.api.json;

import org.commcare.api.util.ApiConstants;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.*;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.form.api.FormEntryPrompt;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Utility functions for performing some action on a Form and receiving a JSON response
 */
public class JsonActionUtils {

    /**
     * Delete a repeat at the specified index, return the JSON response
     *
     * @param controller      the FormEntryController under consideration
     * @param model           the FormEntryModel under consideration
     * @param formIndexString the form index of the repeat group to be deleted
     * @return The JSON representation of the updated form tree
     */
    public static JSONObject deleteRepeatToJson(FormEntryController controller,
                                                FormEntryModel model, String formIndexString) {
        JSONObject ret = new JSONObject();
        FormIndex formIndex = indexFromString(formIndexString, model.getForm());
        controller.deleteRepeat(formIndex);
        ret.put(ApiConstants.QUESTION_TREE_KEY, walkToJSON(model, controller));
        return ret;
    }

    /**
     * Expand (IE create) the repeat at the specified form index
     *
     * @param controller      the FormEntryController under consideration
     * @param model           the FormEntryModel under consideration
     * @param formIndexString the form index of the repeat group to be expanded
     * @return The JSON representation of the updated question tree
     */
    public static JSONObject descendRepeatToJson(FormEntryController controller,
                                                 FormEntryModel model, String formIndexString) {
        JSONObject ret = new JSONObject();
        FormIndex formIndex = indexFromString(formIndexString, model.getForm());
        controller.jumpToIndex(formIndex);
        controller.descendIntoNewRepeat();
        ret.put(ApiConstants.QUESTION_TREE_KEY, walkToJSON(model, controller));
        return ret;
    }

    /**
     * Get the JSON representation of the question tree of this controller/model pair
     *
     * @param controller the FormEntryController under consideration
     * @param model      the FormEntryModel under consideration
     * @return The JSON representation of the question tree
     */
    public static JSONObject getCurrentJson(FormEntryController controller,
                                            FormEntryModel model) {
        JSONObject ret = new JSONObject();
        ret.put(ApiConstants.QUESTION_TREE_KEY, walkToJSON(model, controller));
        return ret;
    }

    /**
     * Answer the question, return the updated JSON representation of the question tree
     *
     * @param controller the FormEntryController under consideration
     * @param model      the FormEntryModel under consideration
     * @param answer     the answer to enter
     * @param prompt     the question to be answered
     * @return The JSON representation of the updated question tree
     */
    public static JSONObject questionAnswerToJson(FormEntryController controller,
                                                  FormEntryModel model, String answer, FormEntryPrompt prompt) {
        JSONObject ret = new JSONObject();
        IAnswerData answerData;

        if (answer == null || answer.equals("None")) {
            answerData = null;
        } else {
            try {
                answerData = getAnswerData(prompt, answer);
            } catch (IllegalArgumentException e) {
                ret.put(ApiConstants.RESPONSE_STATUS_KEY, "error");
                ret.put(ApiConstants.ERROR_TYPE_KEY, "illegal-argument");
                ret.put(ApiConstants.ERROR_REASON_KEY, e.getMessage());
                return ret;
            }
        }
        int result = controller.answerQuestion(prompt.getIndex(), answerData);
        if (result == FormEntryController.ANSWER_REQUIRED_BUT_EMPTY) {
            ret.put(ApiConstants.RESPONSE_STATUS_KEY, "error");
            ret.put(ApiConstants.ERROR_TYPE_KEY, "required");
        } else if (result == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
            ret.put(ApiConstants.RESPONSE_STATUS_KEY, "error");
            ret.put(ApiConstants.ERROR_TYPE_KEY, "restraint");
            ret.put(ApiConstants.ERROR_REASON_KEY, prompt.getConstraintText());
        } else if (result == FormEntryController.ANSWER_OK) {
            ret.put(ApiConstants.QUESTION_TREE_KEY, walkToJSON(model, controller));
            ret.put(ApiConstants.RESPONSE_STATUS_KEY, "accepted");
        }
        return ret;
    }

    /**
     * Answer the question, return the updated JSON representation of the question tree
     *
     * @param controller the FormEntryController under consideration
     * @param model      the FormEntryModel under consideration
     * @param answer     the answer to enter
     * @param index      the form index of the question to be answered
     * @return The JSON representation of the updated question tree
     */
    public static JSONObject questionAnswerToJson(FormEntryController controller,
                                                  FormEntryModel model, String answer, String index) {

        FormIndex formIndex = indexFromString(index, model.getForm());

        FormEntryPrompt prompt = model.getQuestionPrompt(formIndex);
        return questionAnswerToJson(controller, model, answer, prompt);
    }

    /**
     * Return the IAnswerData version of the string data input
     *
     * @param formEntryPrompt the FormEntryPrompt for this question
     * @param data            the String answer
     * @return the IAnswerData version of @data above
     */
    public static IAnswerData getAnswerData(FormEntryPrompt formEntryPrompt, String data) {

        if (formEntryPrompt.getDataType() == Constants.DATATYPE_CHOICE) {
            int index = Integer.parseInt(data);

            SelectChoice ans = formEntryPrompt.getSelectChoices().get(index - 1);

            return new SelectOneData(ans.selection());
        } else if (formEntryPrompt.getDataType() == Constants.DATATYPE_CHOICE_LIST) {
            String[] split = parseMultiSelectString(data);
            Vector<Selection> ret = new Vector<>();
            for (String s : split) {
                int index = Integer.parseInt(s);
                Selection ans = formEntryPrompt.getSelectChoices().get(index - 1).selection();
                ret.add(ans);
            }
            return new SelectMultiData(ret);
        } else if (formEntryPrompt.getDataType() == Constants.DATATYPE_GEOPOINT) {
            return AnswerDataFactory.template(formEntryPrompt.getControlType(), formEntryPrompt.getDataType()).cast(
                    new UncastData(convertTouchFormsGeoPointString(data)));
        }

        return data.equals("") ? null : AnswerDataFactory.template(formEntryPrompt.getControlType(), formEntryPrompt.getDataType()).cast(new UncastData(data));
    }

    // we need to remove the brackets Touchforms includes and replace the commas with spaces
    private static String convertTouchFormsGeoPointString(String touchformsString) {
        return touchformsString.replace(",", " ").replace("[", "").replace("]", "");
    }

    /**
     * OK, this function is kind of a monster. Given a FormDef and a String representation of the form index,
     * return a full fledged FormIndex object.
     */
    public static FormIndex indexFromString(String stringIndex, FormDef form) {
        if (stringIndex == null || stringIndex.equals("None")) {
            return null;
        } else if (stringIndex.equals("<")) {
            return FormIndex.createBeginningOfFormIndex();
        } else if (stringIndex.equals(">")) {
            return FormIndex.createEndOfFormIndex();
        }

        List<Pair<Integer, Integer>> list = stepToList(stringIndex);

        FormIndex ret = reduceFormIndex(list, null);
        ret.assignRefs(form);
        return ret;
    }

    /**
     * Given a String represnetation of a form index, decompose it into a list of <index, multiplicity> pairs
     *
     * @param index the comma separated String representation of the form index
     * @return @index represented as a list of index,multiplicity integer pairs
     */
    private static List<Pair<Integer, Integer>> stepToList(String index) {
        ArrayList<Pair<Integer, Integer>> ret = new ArrayList<>();
        String[] split = index.split(",");
        List<String> list = Arrays.asList(split);
        Collections.reverse(list);
        for (String step : list) {
            if (!step.trim().equals("")) {
                Pair<Integer, Integer> pair = stepFromString(step);
                ret.add(pair);
            }
        }
        return ret;
    }

    /**
     * Given the string representation of one "Step" in a form index, return an Integer pair of <index, multiplicity>
     */
    private static Pair<Integer, Integer> stepFromString(String step) {
        // honestly not sure. thanks obama/drew
        if (step.endsWith("J")) {
            return new Pair<>(Integer.parseInt("" + step.substring(0, step.length() - 1)), TreeReference.INDEX_REPEAT_JUNCTURE);
        }
        // we want to deal with both '.' and '_' as separators for the time being for TF legacy reasons
        String[] split = step.split("[._:]");
        // the form index is the first part, the multiplicity is the second
        int i = Integer.parseInt(split[0].trim());
        int mult = -1;
        if (split.length > 1 && split[1] != null) {
            mult = Integer.parseInt(split[1].trim());
        }
        return new Pair<>(i, mult);
    }

    /**
     * Given a list of steps (see above) to be traversed and a current Form index,
     * pop the top step and create a new FormIndex from this step with the current as its parent, then recursively
     * call this function with the remaining steps and the new FormIndex
     */
    private static FormIndex reduceFormIndex(List<Pair<Integer, Integer>> steps, FormIndex current) {
        if (steps.size() == 0) {
            return current;
        }
        Pair<Integer, Integer> currentStep = steps.remove(0);
        FormIndex nextLevel = new FormIndex(current, currentStep.first, currentStep.second, null);
        return reduceFormIndex(steps, nextLevel);
    }

    private static String[] parseMultiSelectString(String answer) {
        answer = answer.trim();
        if (answer.startsWith("[") && answer.endsWith("]")) {
            answer = answer.substring(1, answer.length() - 1);
        }
        String[] ret = answer.split(" ");
        for (int i = 0; i < ret.length; i++) {
            ret[i] = ret[i].replace(",", "");
        }
        return ret;
    }

    public static JSONArray walkToJSON(FormEntryModel fem, FormEntryController fec) {
        JSONArray ret = new JSONArray();
        FormIndex formIndex = FormIndex.createBeginningOfFormIndex();
        Walker walker = new Walker(ret, formIndex, fec, fem);
        walker.walk();
        return ret;
    }
}
