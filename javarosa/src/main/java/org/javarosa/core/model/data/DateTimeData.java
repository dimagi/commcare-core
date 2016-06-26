package org.javarosa.core.model.data;

import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * A response to a question requesting a DateTime Value
 *
 * @author Clayton Sims
 */
public class DateTimeData implements IAnswerData {
    Date d;

    /**
     * Empty Constructor, necessary for dynamic construction during deserialization.
     * Shouldn't be used otherwise.
     */
    public DateTimeData() {

    }

    public DateTimeData(Date d) {
        setValue(d);
    }

    @Override
    public IAnswerData clone() {
        return new DateTimeData(new Date(d.getTime()));
    }

    @Override
    public void setValue(Object o) {
        //Should not ever be possible to set this to a null value
        if (o == null) {
            throw new NullPointerException("Attempt to set an IAnswerData class to null.");
        }
        d = new Date(((Date)o).getTime());
    }

    @Override
    public Object getValue() {
        return new Date(d.getTime());
    }

    public String getDisplayText() {
        return DateUtils.formatDateTime(d, DateUtils.FORMAT_HUMAN_READABLE_SHORT);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        setValue(ExtUtil.readDate(in));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeDate(out, d);
    }

    @Override
    public UncastData uncast() {
        return new UncastData(DateUtils.formatDateTime(d, DateUtils.FORMAT_ISO8601));
    }

    @Override
    public DateTimeData cast(UncastData data) throws IllegalArgumentException {
        Date ret = DateUtils.parseDateTime(data.value);
        if (ret != null) {
            return new DateTimeData(ret);
        }

        throw new IllegalArgumentException("Invalid cast of data [" + data.value + "] to type DateTime");
    }
}
