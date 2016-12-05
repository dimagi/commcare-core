package org.javarosa.core.model.util.restorable;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;

public interface Restorable {

    void templateData(FormInstance dm, TreeReference parentRef);

}
