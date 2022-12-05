package org.commcare.suite.model;

import static org.commcare.xml.SessionDatumParser.DEFAULT_MAX_SELECT_VAL;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Special kind of EntityDatum that allows for selection of multiple entities in the session
 */
public class MultiSelectEntityDatum extends EntityDatum {

    private int maxSelectValue = DEFAULT_MAX_SELECT_VAL;

    @SuppressWarnings("unused")
    public MultiSelectEntityDatum() {
        // used in serialization
    }

    public MultiSelectEntityDatum(String id, String nodeset, String shortDetail, String longDetail,
            String inlineDetail, String persistentDetail, String value, String autoselect,
            int maxSelectValue) {
        super(id, nodeset, shortDetail, longDetail, inlineDetail, persistentDetail, value,
                autoselect);
        this.maxSelectValue = maxSelectValue;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException,
            DeserializationException {
        super.readExternal(in, pf);
        maxSelectValue = ExtUtil.readInt(in);

        /** Set the correct default here in case the serialised state has
         *  the incorrect older default of -1. This is a temporary work-around
         *  and should be safe to removed ~1 month after it gets deployed **/
        if (maxSelectValue == -1) {
            maxSelectValue = DEFAULT_MAX_SELECT_VAL;
        }
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeNumeric(out, maxSelectValue);
    }

    public int getMaxSelectValue() {
        return maxSelectValue;
    }
}
