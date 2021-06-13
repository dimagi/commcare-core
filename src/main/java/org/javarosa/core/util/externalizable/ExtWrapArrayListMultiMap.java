package org.javarosa.core.util.externalizable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class ExtWrapArrayListMultiMap extends ExternalizableWrapper {

    private ExternalizableWrapper keyType;

    /* Constructors for serialization */

    public ExtWrapArrayListMultiMap(Multimap val) {
        this(val, null);
    }

    public ExtWrapArrayListMultiMap(Multimap val, ExternalizableWrapper keyType) {
        if (val == null) {
            throw new NullPointerException();
        }

        this.val = val;
        this.keyType = keyType;
    }

    /* Constructors for deserialization */

    public ExtWrapArrayListMultiMap() {
    }


    public ExtWrapArrayListMultiMap(Class keyType) {
        this(new ExtWrapBase(keyType));
    }

    public ExtWrapArrayListMultiMap(ExternalizableWrapper keyType) {
        if (keyType == null) {
            throw new NullPointerException();
        }

        this.keyType = keyType;
    }

    @Override
    public ExternalizableWrapper clone(Object val) {
        return new ExtWrapArrayListMultiMap((Multimap)val, keyType);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        long size = ExtUtil.readNumeric(in);
        ArrayListMultimap<Object, Object> multimap = ArrayListMultimap.create();
        for (int i = 0; i < size; i++) {
            Object key = ExtUtil.read(in, keyType, pf);

            long numberOfValues = ExtUtil.readNumeric(in);
            for (long l = 0; l < numberOfValues; l++) {
                multimap.put(key, ExtUtil.read(in, new ExtWrapTagged(), pf));
            }
        }
        val = multimap;
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ArrayListMultimap multimap = (ArrayListMultimap)val;
        ExtUtil.writeNumeric(out, multimap.keySet().size());
        for (Object key : multimap.keySet()) {
            ExtUtil.write(out, keyType == null ? key : keyType.clone(key));
            List values = multimap.get(key);
            ExtUtil.writeNumeric(out, values.size());
            for (int i = 0; i < values.size(); i++) {
                ExtUtil.write(out, new ExtWrapTagged(values.get(i)));
            }
        }
    }

    @Override
    public void metaReadExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        keyType = ExtWrapTagged.readTag(in, pf);
    }

    @Override
    public void metaWriteExternal(DataOutputStream out) throws IOException {
        Multimap multimap = (Multimap)val;
        Object keyTagObj = (keyType == null ? (multimap.size() == 0 ? new Object() : multimap.keys().iterator().next()) : keyType);
        ExtWrapTagged.writeTag(out, keyTagObj);
    }
}
