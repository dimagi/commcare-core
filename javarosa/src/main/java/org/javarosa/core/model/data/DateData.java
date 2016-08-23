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
 * A response to a question requesting a Date Value
 *
 * @author Drew Roos
 */
public class DateData implements IAnswerData {
    Date d;
    boolean init = false;

    /**
     * Empty Constructor, necessary for dynamic construction during deserialization.
     * Shouldn't be used otherwise.
     */
    public DateData() {

    }

    public DateData(Date d) {
        setValue(d);
    }

    private void init() {
        if (!init) {
            d = DateUtils.roundDate(d);
            init = true;
        }
    }

    @Override
    public IAnswerData clone() {
        init();
        return new DateData(new Date(d.getTime()));
    }

    @Override
    public void setValue(Object o) {
        //Should not ever be possible to set this to a null value
        if (o == null) {
            throw new NullPointerException("Attempt to set an IAnswerData class to null.");
        }
        d = (Date)o;
        init = false;
    }

    @Override
    public Object getValue() {
        init();
        return new Date(d.getTime());
    }

    @Override
    public String getDisplayText() {
        init();
        return DateUtils.formatDate(d, DateUtils.FORMAT_HUMAN_READABLE_SHORT);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        setValue(ExtUtil.readDate(in));
        init();
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        init();
        ExtUtil.writeDate(out, d);
    }

    @Override
    public UncastData uncast() {
        init();
        return new UncastData(DateUtils.formatDate(d, DateUtils.FORMAT_ISO8601));
    }

    @Override
    public DateData cast(UncastData data) throws IllegalArgumentException {
        Date ret = DateUtils.parseDate(data.value);
        if (ret != null) {
            return new DateData(ret);
        }

        throw new IllegalArgumentException("Invalid cast of data [" + data.value + "] to type Date");
    }
}
