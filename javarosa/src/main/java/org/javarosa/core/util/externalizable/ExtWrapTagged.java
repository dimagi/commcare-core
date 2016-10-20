package org.javarosa.core.util.externalizable;

import org.javarosa.core.model.data.LargeString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

public class ExtWrapTagged extends ExternalizableWrapper {

    private static final Hashtable<Class, Integer> WRAPPER_CODES;

    static {
        WRAPPER_CODES = new Hashtable<>();
        WRAPPER_CODES.put(ExtWrapNullable.class, 0x00);
        WRAPPER_CODES.put(ExtWrapList.class, 0x20);
        WRAPPER_CODES.put(ExtWrapListPoly.class, 0x21);
        WRAPPER_CODES.put(ExtWrapMap.class, 0x22);
        WRAPPER_CODES.put(ExtWrapMapPoly.class, 0x23);
        WRAPPER_CODES.put(ExtWrapIntEncodingUniform.class, 0x40);
        WRAPPER_CODES.put(ExtWrapIntEncodingSmall.class, 0x41);
        WRAPPER_CODES.put(LargeString.class, 0x42);
    }

    /* serialization */

    public ExtWrapTagged(Object val) {
        if (val == null) {
            throw new NullPointerException();
        } else if (val instanceof ExtWrapTagged) {
            throw new IllegalArgumentException("Wrapping tagged with tagged is redundant");
        }

        this.val = val;
    }

    /* deserialization */

    public ExtWrapTagged() {

    }

    @Override
    public ExternalizableWrapper clone(Object val) {
        return new ExtWrapTagged(val);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        ExternalizableWrapper type = readTag(in, pf);
        val = ExtUtil.read(in, type, pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        writeTag(out, val);
        ExtUtil.write(out, val);
    }

    public static ExternalizableWrapper readTag(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        byte[] tag = new byte[PrototypeFactory.getClassHashSize()];
        in.read(tag, 0, tag.length);

        if (PrototypeFactory.compareHash(tag, PrototypeFactory.getWrapperTag())) {
            int wrapperCode = ExtUtil.readInt(in);

            //find wrapper indicated by code
            ExternalizableWrapper type = null;
            for (Enumeration e = WRAPPER_CODES.keys(); e.hasMoreElements(); ) {
                Class t = (Class)e.nextElement();
                if ((WRAPPER_CODES.get(t)).intValue() == wrapperCode) {
                    try {
                        type = (ExternalizableWrapper)PrototypeFactory.getInstance(t);
                    } catch (CannotCreateObjectException ccoe) {
                        throw new CannotCreateObjectException("Serious problem: cannot create built-in ExternalizableWrapper [" + t.getName() + "]");
                    }
                }
            }
            if (type == null) {
                throw new DeserializationException("Unrecognized ExternalizableWrapper type [" + wrapperCode + "]");
            }

            type.metaReadExternal(in, pf);
            return type;
        } else {
            Class type = pf.getClass(tag);
            if (type == null) {
                throw new DeserializationException("No datatype registered to serialization code " + ExtUtil.printBytes(tag));
            }

            return new ExtWrapBase(type);
        }
    }

    public static void writeTag(DataOutputStream out, Object o) throws IOException {
        if (o instanceof ExternalizableWrapper && !(o instanceof ExtWrapBase)) {
            out.write(PrototypeFactory.getWrapperTag(), 0, PrototypeFactory.getClassHashSize());
            ExtUtil.writeNumeric(out, WRAPPER_CODES.get(o.getClass()));
            ((ExternalizableWrapper)o).metaWriteExternal(out);
        } else {
            Class type = null;

            if (o instanceof ExtWrapBase) {
                ExtWrapBase extType = (ExtWrapBase)o;
                if (extType.val != null) {
                    o = extType.val;
                } else {
                    type = extType.type;
                }
            }
            if (type == null) {
                type = o.getClass();
            }

            byte[] tag = PrototypeFactory.getClassHash(type); //cache this?
            out.write(tag, 0, tag.length);
        }
    }

    @Override
    public void metaReadExternal(DataInputStream in, PrototypeFactory pf) {
        throw new RuntimeException("Tagged wrapper should never be tagged");
    }

    @Override
    public void metaWriteExternal(DataOutputStream out) {
        throw new RuntimeException("Tagged wrapper should never be tagged");
    }
}
