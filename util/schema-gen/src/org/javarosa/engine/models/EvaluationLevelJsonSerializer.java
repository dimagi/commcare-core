/**
 * 
 */
package org.javarosa.engine.models;

import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.core.model.trace.EvaluationTraceSerializer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * @author ctsims
 *
 */
public class EvaluationLevelJsonSerializer implements EvaluationTraceSerializer<String> {
    
    public String serializeEvaluationLevels(EvaluationTrace input) {
        JSONObject object = aggregateJSON(input);
        return object.toJSONString();
    }
    
    public JSONObject aggregateJSON(EvaluationTrace level) {
        JSONObject object = new JSONObject();
        object.put("expression", level.getExpression());
        object.put("value", level.getValue());
        JSONArray array = new JSONArray();
        for(EvaluationTrace child : level.getSubTraces()) {
            array.add(aggregateJSON(child));
        }
        object.put("components", array);
        return object;
    }
    
    public String tabLevel(int tabLevel) {
        String level = "";
        for(int i = 0; i < tabLevel; ++i) {
            level +="    ";
        }
        return level;
    }
}
