package org.commcare.api.json;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by willpride on 12/8/15.
 */
public class Walker {

    JSONArray compiler;
    FormIndex parentIndex;
    FormEntryController fec;
    FormEntryModel fem;

    public Walker(JSONArray compiler, FormIndex parentIndex, FormEntryController fec, FormEntryModel fem){
        this.compiler = compiler;
        this.parentIndex = parentIndex;
        this.fec = fec;
        this.fem = fem;
    }

    public FormIndex step(FormIndex formIndex, boolean descend){
        FormIndex nextIndex = fec.getAdjacentIndex(formIndex, true, descend);
        fem.setQuestionIndex(nextIndex);
        return nextIndex;
    }

    public boolean indexInScope(FormIndex formIndex){
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

            obj.put("relevant", relevant);

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
