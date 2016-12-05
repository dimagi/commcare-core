package org.javarosa.core.model.utils;

import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.DecimalData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.LongData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.StringData;

import java.util.Date;
import java.util.Vector;

/**
 * @author Clayton Sims
 * @date Mar 30, 2009
 */
public class PreloadUtils {

    /**
     * Note: This method is a hack to fix the problem that we don't know what
     * data type we're using when we have a preloader. That should get fixed,
     * and this method should be removed.
     */
    public static IAnswerData wrapIndeterminedObject(Object o) {
        if (o == null) {
            return null;
        }

        //TODO: Replace this all with an uncast data
        if (o instanceof String) {
            return new StringData((String)o);
        } else if (o instanceof Date) {
            return new DateData((Date)o);
        } else if (o instanceof Integer) {
            return new IntegerData((Integer)o);
        } else if (o instanceof Long) {
            return new LongData((Long)o);
        } else if (o instanceof Double) {
            return new DecimalData((Double)o);
        } else if (o instanceof Vector) {
            return new SelectMultiData((Vector)o);
        } else if (o instanceof IAnswerData) {
            return (IAnswerData)o;
        }
        return new StringData(o.toString());
    }
}
