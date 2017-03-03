package org.commcare.util;

import org.commcare.cases.entity.Entity;
import org.commcare.suite.model.DetailField;

/**
 * Contains all of the raw information needed by a PrintableDetailField
 *
 * Created by amstone326 on 3/2/17.
 */
public class DetailFieldPrintInfo {

    public DetailField field;
    public Entity entity;
    public int index;

    public DetailFieldPrintInfo(DetailField field, Entity entity, int fieldIndex) {
        this.field = field;
        this.entity = entity;
        this.index = fieldIndex;
    }
}
