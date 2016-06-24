package org.javarosa.core.model.instance.utils;

import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.utils.IInstanceVisitor;

/**
 * ITreeVisitor is a visitor interface for the elements of the
 * FormInstance tree elements. In the case of composite elements,
 * method dispatch for composite members occurs following dispatch
 * for the composing member.
 *
 * @author Clayton Sims
 */
public interface ITreeVisitor extends IInstanceVisitor {
    void visit(FormInstance tree);

    void visit(AbstractTreeElement element);
}
