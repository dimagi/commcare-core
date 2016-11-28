package org.javarosa.core.model;

import org.javarosa.core.model.condition.Condition;
import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.condition.Recalculate;
import org.javarosa.model.xform.XPathReference;

/**
 * A data binding is an object that represents how a
 * data element is to be used in a form entry interaction.
 *
 * It contains a reference to where the data should be retreived
 * and stored, as well as the preload parameters, and the
 * conditional logic for the question.
 *
 * The class relies on any Data References that are used
 * in a form to be registered with the FormDefRMSUtility's
 * prototype factory in order to properly deserialize.
 *
 * @author Drew Roos
 */
public class DataBinding {
    private String id;
    private XPathReference ref;
    private int dataType;

    public Condition relevancyCondition;
    public boolean relevantAbsolute;
    public Condition requiredCondition;
    public boolean requiredAbsolute;
    public Condition readonlyCondition;
    public boolean readonlyAbsolute;
    public IConditionExpr constraint;
    public Recalculate calculate;

    private String preload;
    private String preloadParams;
    public String constraintMessage;

    public DataBinding() {
        relevantAbsolute = true;
        requiredAbsolute = false;
        readonlyAbsolute = false;
    }

    public XPathReference getReference() {
        return ref;
    }

    public void setReference(XPathReference ref) {
        this.ref = ref;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public String getPreload() {
        return preload;
    }

    public void setPreload(String preload) {
        this.preload = preload;
    }

    public String getPreloadParams() {
        return preloadParams;
    }

    public void setPreloadParams(String preloadParams) {
        this.preloadParams = preloadParams;
    }
}
