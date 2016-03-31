package org.commcare.api.json;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The Walker class is kind of a beast. At the highest level, a Walker is responsible for transforming
 * a given state and index during form entry (as represented by a FormEntryController, FormEntryModel, and Form Index)
 * into a JSON representation. For each question at the Walker's starting depth, the Walker will generate a JSON
 * representation of this question and add it to the compiler array, expanding repeats where necessary.
 * For each subgroup, the Walker will recursively instantiate another Walker for this group and add its output as a child.
 * Each Walker might itself be a subgroup of another Walker.
 *
 * @author wpride
 */
public class Walker {

    private final JSONArray compiler;
    private final FormIndex parentIndex;
    private final FormEntryController fec;
    private final FormEntryModel fem;

    public Walker(JSONArray compiler, FormIndex parentIndex, FormEntryController fec, FormEntryModel fem){
        this.compiler = compiler;
        this.parentIndex = parentIndex;
        this.fec = fec;
        this.fem = fem;
    }

    private FormIndex step(FormIndex formIndex, boolean descend){
        FormIndex nextIndex = fec.getAdjacentIndex(formIndex, true, descend);
        fem.setQuestionIndex(nextIndex);
        return nextIndex;
    }

    private boolean indexInScope(FormIndex formIndex){
        if(formIndex.isEndOfFormIndex()){
            return false;
        }
        if(parentIndex.isBeginningOfFormIndex()){
            return true;
        }
        return FormIndex.isSubElement(parentIndex, formIndex);
    }

    public FormIndex walk(){
        FormIndex currentIndex = step(parentIndex, true);
        while(indexInScope(currentIndex)){
            boolean relevant = fem.isIndexRelevant(currentIndex);

            if(!relevant){
                currentIndex = step(currentIndex, false);
                break;
            }

            JSONObject obj = new JSONObject();
            PromptToJson.parseQuestionType(fem, obj);

            if(obj.get("type").equals("sub-group")){
                Walker walker;
                if(obj.has("caption") && obj.get("caption") != JSONObject.NULL  ){
                    JSONArray childObject = new JSONArray();
                    walker = new Walker(childObject, currentIndex, fec, fem);
                    obj.put("children", childObject);
                } else{
                    walker = new Walker(compiler, currentIndex, fec, fem);
                }
                compiler.put(obj);
                currentIndex = walker.walk();
            } else if(obj.get("type").equals("repeat-juncture")){
                JSONArray children = new JSONArray();
                for(int i = 0; i < fem.getForm().getNumRepetitions(currentIndex); i++){
                    JSONObject subEvent = new JSONObject();
                    FormIndex ix = fem.getForm().descendIntoRepeat(currentIndex, i);
                    JSONArray subChildren = new JSONArray();
                    subEvent.put("type", "sub-group");
                    subEvent.put("ix", ix);
                    JSONArray repetitionsArray = obj.getJSONArray("repetitions");
                    subEvent.put("caption", repetitionsArray.get(i));
                    subEvent.put("repeatable", true);
                    subEvent.put("uuid", ix.getReference()).hashCode();
                    Walker walker = new Walker(subChildren, ix, fec, fem);
                    walker.walk();
                    subEvent.put("children", subChildren);
                    children.put(subEvent);
                }
                obj.put("children", children);
                compiler.put(obj);
                currentIndex = step(currentIndex, true);
            } else{
                compiler.put(obj);
                currentIndex = step(currentIndex, true);
            }
        }
        return currentIndex;
    }
}
