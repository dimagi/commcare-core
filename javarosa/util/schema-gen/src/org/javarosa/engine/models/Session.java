package org.javarosa.engine.models;

import org.javarosa.core.util.PropertyUtils;

import java.util.Vector;


/**
 * @author ctsims
 *
 */
public class Session {

    final String uuid;
    final Vector<Step> steps;

    public Session() {
        uuid = PropertyUtils.genUUID();
        steps = new Vector();
    }

    public Vector<Step> getSteps() {
        return steps;
    }

    public void addStep(Step step) {
        steps.addElement(step);
    }
}
