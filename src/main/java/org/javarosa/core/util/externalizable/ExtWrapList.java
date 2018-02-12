package org.javarosa.core.util.externalizable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;


// List of objects of single (non-polymorphic) type
public class ExtWrapList extends ExternalizableWrapper {
    public ExternalizableWrapper type;
    private boolean sealed;
    private Class<? extends List> listImplementation;

    /* serialization */

    public ExtWrapList(List val) {
        this(val, null);
    }

    public ExtWrapList(List val, ExternalizableWrapper type) {
        if (val == null) {
            throw new NullPointerException();
        }

        this.val = val;
        this.type = type;
        this.listImplementation = val.getClass();
    }

    /* deserialization */

    public ExtWrapList() {

    }

    public ExtWrapList(Class listElementType) {
        this(listElementType, Vector.class);
    }

    public ExtWrapList(Class listElementType, Class listImplementation) {
        this.type = new ExtWrapBase(listElementType);
        this.listImplementation = listImplementation;
        this.sealed = false;
    }

    public ExtWrapList(ExternalizableWrapper type) {
        if (type == null) {
            throw new NullPointerException();
        }

        this.listImplementation = Vector.class;
        this.type = type;
    }

    @Override
    public ExternalizableWrapper clone(Object val) {
        return new ExtWrapList((List)val, type);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        if (!sealed) {
            int size = (int)ExtUtil.readNumeric(in);
            try {
                List<Object> l = listImplementation.newInstance();
                for (int i = 0; i < size; i++) {
                    l.add(ExtUtil.read(in, type, pf));
                }
                val = l;
            } catch (InstantiationException e) {
                throw new DeserializationException("caused by: " + e.getClass().getCanonicalName());
            } catch (IllegalAccessException e) {
                throw new DeserializationException("caused by: " + e.getClass().getCanonicalName());
            }
        } else {
            int size = (int)ExtUtil.readNumeric(in);
            Object[] theval = new Object[size];
            for (int i = 0; i < size; i++) {
                theval[i] = ExtUtil.read(in, type, pf);
            }
            val = theval;
        }
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        List l = (List)val;
        ExtUtil.writeNumeric(out, l.size());
        for (int i = 0; i < l.size(); i++) {
            ExtUtil.write(out, type == null ? l.get(i) : type.clone(l.get(i)));
        }
    }

    @Override
    public void metaReadExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        type = ExtWrapTagged.readTag(in, pf);
        try {
            listImplementation = (Class<? extends List>)Class.forName(ExtUtil.readString(in));
        } catch (ClassNotFoundException e) {
            throw new DeserializationException("caused by: " + e);
        }
    }

    @Override
    public void metaWriteExternal(DataOutputStream out) throws IOException {
        List l = (List)val;
        Object tagObj;

        if (type == null) {
            if (l.size() == 0) {
                tagObj = new Object();
            } else {
                tagObj = l.get(0);
            }
        } else {
            tagObj = type;
        }

        ExtWrapTagged.writeTag(out, tagObj);
        ExtUtil.writeString(out, listImplementation.getName());
    }
}
