package org.commcare.api.json;

import org.commcare.modern.util.Pair;
import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.AnswerDataFactory;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.form.api.FormEntryPrompt;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * Created by willpride on 11/18/15.
 */
public class AnswerQuestionJson {

    public static JSONObject questionAnswerToJson(FormEntryController controller,
                                                  FormEntryModel model, String answer, FormEntryPrompt prompt){
        JSONObject ret = new JSONObject();
        IAnswerData answerData = null;

        if(answer.equals("None")){
            answerData = null;
        } else {
            try {
                answerData = getAnswerData(prompt, answer);
            } catch (IllegalArgumentException e) {
                ret.put("status", "error");
                ret.put("type", "illegal-argument");
                ret.put("reason", e.getMessage());
                return ret;
            }
        }
        int result = controller.answerQuestion(prompt.getIndex(), answerData);
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

    public static String questionAnswerToJson(FormEntryController controller,
                                                  FormEntryModel model, String answer, String index){
        System.out.println("Java Answer: " + answer + " index " + index);
        try {
            FormIndex formIndex = indexFromString(index, model.getForm());
            FormEntryPrompt prompt = model.getQuestionPrompt(formIndex);
            return questionAnswerToJson(controller, model, answer, prompt).toString();
        } catch(Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public static JSONObject questionAnswerToJson(FormEntryController controller,
                                                  FormEntryModel model, String answer){
        FormEntryPrompt prompt = model.getQuestionPrompt();
        return questionAnswerToJson(controller, model, answer, prompt);
    }

    public static IAnswerData getAnswerData(FormEntryPrompt fep, String data){

        if(fep.getDataType() == Constants.DATATYPE_CHOICE){
            int index = Integer.parseInt(data);
            SelectChoice ans = fep.getSelectChoices().get(index -1);
            return new SelectOneData(ans.selection());
        } else if(fep.getDataType() == Constants.DATATYPE_CHOICE_LIST){
            String[] split = parseMultiSelectString(data);
            Vector<Selection> ret = new Vector<>();
            for (String s: split){
                int index = Integer.parseInt(s);
                Selection ans = fep.getSelectChoices().get(index -1).selection();
                ret.add(ans);
            }
            return new SelectMultiData(ret);
        }

        return data.equals("") ? null : AnswerDataFactory.template(fep.getControlType(), fep.getDataType()).cast(new UncastData(data));
    }

    public static Pair<Integer, Integer> stepFromString(String step){

        System.out.println("Java Step from string: " + step);

        if(step.endsWith("J")){
            return new Pair<>(Integer.getInteger("" + step.charAt(step.length())), -10);
        }
        String[] split = step.split("[._]");

        System.out.println("Split: " + Arrays.toString(split));

        int i = Integer.parseInt(split[0].trim());
        int mult = -1;
        try{
            mult = Integer.parseInt(split[1].trim());
        } catch(IndexOutOfBoundsException | NullPointerException e){
            System.out.println("E: " + e.getMessage());
        }
        return new Pair<>(i, mult);
    }

    public static List<Pair<Integer, Integer>> stepToList(String index){
        ArrayList<Pair<Integer, Integer>> ret = new ArrayList<Pair<Integer, Integer>>();
        String[] split = index.split(",");
        List<String> list = Arrays.asList(split);
        Collections.reverse(list);
        for(String step: list){
            if(!step.trim().equals("")) {
                Pair<Integer, Integer> pair = stepFromString(step);
                System.out.println("Java Pair: " + pair);
                ret.add(pair);
            }
        }
        return ret;
    }

    public static FormIndex reduceFormIndex(List<Pair<Integer, Integer>> steps, FormIndex current){
        if(steps.size() == 0){
            return current;
        }
        Pair<Integer, Integer> currentStep = steps.remove(0);
        FormIndex nextLevel = new FormIndex(current, currentStep.first, currentStep.second, null);
        return reduceFormIndex(steps, nextLevel);
    }

    public static FormIndex indexFromString(String stringIndex, FormDef form){
        if(stringIndex == null || stringIndex.equals("None")){
            return null;
        } else if(stringIndex.equals("<")){
            return FormIndex.createBeginningOfFormIndex();
        } else if(stringIndex.equals(">")){
            return FormIndex.createEndOfFormIndex();
        }
        FormIndex ret = reduceFormIndex(stepToList(stringIndex), null);
        ret.assignRefs(form);
        return ret;
    }

    public static String[] parseMultiSelectString(String answer){
        answer = answer.trim();
        if(answer.startsWith("[") && answer.endsWith("]")){
            answer = answer.substring(1, answer.length()-1);
        }
        String[] ret = answer.split(" ");
        for(int i=0; i< ret.length; i++){
            ret[i] = ret[i].replace(",","");
        }
        return ret;
    }
}
