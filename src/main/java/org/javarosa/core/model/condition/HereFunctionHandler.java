package org.javarosa.core.model.condition;

import java.util.Vector;

/**
 * Created by amstone326 on 12/6/17.
 */
public abstract class HereFunctionHandler implements IFunctionHandler {

    protected HereFunctionHandlerListener listener;

    @Override
    public Vector getPrototypes() {
        Vector<Class[]> p = new Vector<>();
        p.addElement(new Class[0]);
        return p;
    }

    @Override
    public boolean rawArgs() {
        return false;
    }

    @Override
    public String getName() {
        return "here";
    }

    public void registerListener(HereFunctionHandlerListener listener) {
        this.listener = listener;
    }

    public void unregisterListener() {
        this.listener = null;
    }

    protected void alertOnEval() {
        if (listener != null) {
            listener.onHereFunctionEvaluated();
        }
    }
}
