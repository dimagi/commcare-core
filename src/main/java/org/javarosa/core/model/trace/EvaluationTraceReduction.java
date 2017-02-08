package org.javarosa.core.model.trace;

import org.javarosa.core.util.ArrayUtilities;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.expr.FunctionUtils;

import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

/**
 * A Trace Reduction represents a "folded-in" model of an evaluation trace
 * which aggregates stats about multiple traces which followed the same structure
 *
 * Created by ctsims on 1/24/2017.
 */

public class EvaluationTraceReduction extends EvaluationTrace {
    String expression;

    int countExecuted = 0;
    long nanoTime = 0;

    HashMap<String, Integer> valueMap = new HashMap<>();

    OrderedHashtable<String, EvaluationTraceReduction> subTraces = new OrderedHashtable<>();

    Vector<EvaluationTraceReduction> children = new Vector<>();

    public EvaluationTraceReduction(EvaluationTrace trace) {
        super(trace.getExpression());
        this.expression = trace.getExpression();
        foldIn(trace);
    }

    public void foldIn(EvaluationTrace trace) {
        countExecuted ++;
        nanoTime += trace.getRuntimeInNanoseconds();
        int valueCount = 1;
        if(valueMap.containsKey(trace.getValue())) {
            valueCount = (valueMap.get(trace.getValue()) + 1);
        }
        valueMap.put(trace.getValue(), valueCount);
        Vector<EvaluationTrace> subTraceVector = trace.getSubTraces();
        Vector<EvaluationTrace> copy = (Vector)subTraceVector.clone();
        synchronized (subTraceVector) {
            try {
                for (EvaluationTrace subTrace : copy) {
                    String subKey = subTrace.getExpression();
                    if (subTraces.containsKey(subKey)) {
                        EvaluationTraceReduction reducedSubExpr = subTraces.get(subTrace.getExpression());
                        reducedSubExpr.foldIn(subTrace);
                    } else {
                        EvaluationTraceReduction reducedSubExpr = new EvaluationTraceReduction(subTrace);
                        subTraces.put(subKey, reducedSubExpr);
                    }
                }
            }catch (ConcurrentModificationException cme) {
                throw new RuntimeException(cme);
            }
        }
    }


    public Vector<EvaluationTrace> getSubTraces() {
        return new Vector<EvaluationTrace>(subTraces.values());
    }

    public String getExpression() {
        return expression;
    }

    /**
     * @return The outcome of the expression's execution.
     */
    public String getValue() {
        return String.valueOf(countExecuted);
    }

    protected long getRuntimeInNanoseconds() {
        return nanoTime;
    }


    public String getProfileReport() {
        String response = "{\n";
        response +=  "    time: " + getRuntimeCount(getRuntimeInNanoseconds()) + "\n";
        response +=  "    time/call: " + getRuntimeCount(getRuntimeInNanoseconds() / countExecuted) + "\n";
        int valueResponseCount = 0;
        int totalRecords = valueMap.size();
        for(String key : valueMap.keySet()) {
            response += "    " + key + ": " + valueMap.get(key) + "\n";
            valueResponseCount++;
            if(valueResponseCount >= 10) {
                response += String.format("    ... %s more ...", totalRecords - valueResponseCount);
                break;
            }
        }
        response += "}";
        return response;
    }

    private String getRuntimeCount(long l) {
        if(l / 1000 / 1000 > 0) {
            return l /1000 / 1000 + "ms";
        } else if(l / 1000 > 0) {
            return l / 1000 + "us";
        }else {
            return l + "ns";
        }
    }
}
