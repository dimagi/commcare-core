package org.javarosa.core.model.condition;

/**
 * Created by amstone326 on 12/6/17.
 */

public interface HereFunctionHandlerListener {
    void onEvalLocationChanged();
    void onHereFunctionEvaluated();
    String getLocation();
}
