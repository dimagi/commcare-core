package org.javarosa.core.util.test;

import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapBase;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapMapPoly;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.ExternalizableWrapper;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ExternalizableTest {
    public static void testExternalizable(Object orig, Object template, PrototypeFactory pf, String failMessage) {
        if (failMessage == null) {
            failMessage = "Serialization Failure";
        }

        byte[] bytes;
        Object deser;

        print("");
        print("Original: " + printObj(orig));

        try {
            bytes = ExtUtil.serialize(orig);

            print("Serialized as:");
            print(ExtUtil.printBytes(bytes));

            if (template instanceof Class) {
                deser = ExtUtil.deserialize(bytes, (Class)template, pf);
            } else if (template instanceof ExternalizableWrapper) {
                deser = ExtUtil.read(new DataInputStream(new ByteArrayInputStream(bytes)), (ExternalizableWrapper)template, pf);
            } else {
                throw new ClassCastException();
            }

            print("Reconstituted: " + printObj(deser));

            if (ExtUtil.equals(orig, deser, true)) {
                print("SUCCESS");
            } else {
                print("FAILURE");
                fail(failMessage + ": Objects do not match");
            }
            print("---------------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
            fail(failMessage + ": Exception! " + e.getClass().getName() + " " + e.getMessage());
        }
    }

    public static void testExternalizable(Externalizable original, PrototypeFactory pf, String failMessage) {
        testExternalizable(original, original.getClass(), pf, failMessage);
    }

    //for use inside this test suite
    public void testExternalizable(Object orig, Object template) {
        testExternalizable(orig, template, null);
    }

    public void testExternalizable(Object orig, Object template, PrototypeFactory pf) {
        testExternalizable(orig, template, pf, null);
    }

    public static String printObj(Object o) {
        o = ExtUtil.unwrap(o);

        if (o == null) {
            return "(null)";
        } else if (o instanceof Vector) {
            StringBuffer sb = new StringBuffer();
            sb.append("V[");
            for (Enumeration e = ((Vector)o).elements(); e.hasMoreElements(); ) {
                sb.append(printObj(e.nextElement()));
                if (e.hasMoreElements())
                    sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        } else if (o instanceof Hashtable) {
            StringBuffer sb = new StringBuffer();
            sb.append((o instanceof OrderedHashtable ? "oH" : "H") + "[");
            for (Enumeration e = ((Hashtable)o).keys(); e.hasMoreElements(); ) {
                Object key = e.nextElement();
                sb.append(printObj(key));
                sb.append("=>");
                sb.append(printObj(((Hashtable)o).get(key)));
                if (e.hasMoreElements())
                    sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        } else {
            return "{" + o.getClass().getName() + ":" + o.toString() + "}";
        }
    }

    private static void print(String s) {
        System.out.println(s);
    }

    @Test
    public void doTests() {
        //base types (built-in + externalizable)

        PrototypeFactory pf = new PrototypeFactory();
        PrototypeFactory.setStaticHasher(new ClassNameHasher());

        testExternalizable("string", String.class);
        testExternalizable(new Byte((byte)0), Byte.class);
        testExternalizable(new Byte((byte)0x55), Byte.class);
        testExternalizable(new Byte((byte)0xe9), Byte.class);
        testExternalizable(new Short((short)0), Short.class);
        testExternalizable(new Short((short)-12345), Short.class);
        testExternalizable(new Short((short)12345), Short.class);
        testExternalizable(new Integer(0), Integer.class);
        testExternalizable(new Integer(1234567890), Integer.class);
        testExternalizable(new Integer(-1234567890), Integer.class);
        testExternalizable(new Long(0), Long.class);
        testExternalizable(new Long(1234567890123456789L), Long.class);
        testExternalizable(new Long(-1234567890123456789L), Long.class);
        testExternalizable(Boolean.TRUE, Boolean.class);
        testExternalizable(Boolean.FALSE, Boolean.class);
        testExternalizable(new Character('e'), Character.class);
        testExternalizable(new Float(123.45e6), Float.class);
        testExternalizable(new Double(123.45e6), Double.class);
        testExternalizable(new Date(), Date.class);
        testExternalizable(new SampleExtz("your", "mom"), SampleExtz.class);

        //base wrapper (end user will never use)
        testExternalizable("string", new ExtWrapBase(String.class));
        testExternalizable(new ExtWrapBase("string"), String.class);

        //nullables on base types
        testExternalizable(new ExtWrapNullable((String)null), new ExtWrapNullable(String.class));
        testExternalizable(new ExtWrapNullable("string"), new ExtWrapNullable(String.class));
        testExternalizable(new ExtWrapNullable((Integer)null), new ExtWrapNullable(Integer.class));
        testExternalizable(new ExtWrapNullable(new Integer(17)), new ExtWrapNullable(Integer.class));
        testExternalizable(new ExtWrapNullable((SampleExtz)null), new ExtWrapNullable(SampleExtz.class));
        testExternalizable(new ExtWrapNullable(new SampleExtz("hi", "there")), new ExtWrapNullable(SampleExtz.class));

        //vectors of base types
        Vector v = new Vector();
        v.addElement(new Integer(27));
        v.addElement(new Integer(-73));
        v.addElement(new Integer(1024));
        v.addElement(new Integer(66066066));
        testExternalizable(new ExtWrapList(v), new ExtWrapList(Integer.class));

        Vector vs = new Vector();
        vs.addElement("alpha");
        vs.addElement("beta");
        vs.addElement("gamma");
        testExternalizable(new ExtWrapList(vs), new ExtWrapList(String.class));

        Vector w = new Vector();
        w.addElement(new SampleExtz("where", "is"));
        w.addElement(new SampleExtz("the", "beef"));
        testExternalizable(new ExtWrapList(w), new ExtWrapList(SampleExtz.class));

        //nullable vectors; vectors of nullables (no practical use)
        testExternalizable(new ExtWrapNullable(new ExtWrapList(v)), new ExtWrapNullable(new ExtWrapList(Integer.class)));
        testExternalizable(new ExtWrapNullable((ExtWrapList)null), new ExtWrapNullable(new ExtWrapList(Integer.class)));
        testExternalizable(new ExtWrapList(v, new ExtWrapNullable()), new ExtWrapList(new ExtWrapNullable(Integer.class)));

        //empty vectors (base types)
        testExternalizable(new ExtWrapList(new Vector()), new ExtWrapList(String.class));
        testExternalizable(new ExtWrapList(new Vector(), new ExtWrapBase(Integer.class)), new ExtWrapList(String.class)); //sub-types don't matter for empties

        //vectors of vectors (including empties)
        Vector x = new Vector();
        x.addElement(new Integer(-35));
        x.addElement(new Integer(-31415926));
        Vector y = new Vector();
        y.addElement(v);
        y.addElement(x);
        y.addElement(new Vector());
        testExternalizable(new ExtWrapList(y, new ExtWrapList()), new ExtWrapList(new ExtWrapList(Integer.class))); //risky to not specify 'leaf' type (Integer), but works in limited situations
        testExternalizable(new ExtWrapList(new Vector(), new ExtWrapList()), new ExtWrapList(new ExtWrapList(Integer.class))); //same as above

        //tagged base types
        testExternalizable(new ExtWrapTagged("string"), new ExtWrapTagged());
        testExternalizable(new ExtWrapTagged(new Integer(5000)), new ExtWrapTagged());
        //tagged custom type
        //PrototypeFactory pf = new PrototypeFactory();
        pf.addClass(SampleExtz.class);
        testExternalizable(new ExtWrapTagged(new SampleExtz("bon", "jovi")), new ExtWrapTagged(), pf);
        //tagged vector (base type)
        testExternalizable(new ExtWrapTagged(new ExtWrapList(v)), new ExtWrapTagged(), pf);
        testExternalizable(new ExtWrapTagged(new ExtWrapList(w)), new ExtWrapTagged(), pf);
        //tagged nullables and compound vectors
        testExternalizable(new ExtWrapTagged(new ExtWrapNullable("string")), new ExtWrapTagged());
        testExternalizable(new ExtWrapTagged(new ExtWrapNullable((String)null)), new ExtWrapTagged());
        testExternalizable(new ExtWrapTagged(new ExtWrapList(y, new ExtWrapList(Integer.class))), new ExtWrapTagged());
        testExternalizable(new ExtWrapTagged(new ExtWrapList(new Vector(), new ExtWrapList(Integer.class))), new ExtWrapTagged());

        //polymorphic vectors
        Vector a = new Vector();
        a.addElement(new Integer(47));
        a.addElement("string");
        a.addElement(Boolean.FALSE);
        a.addElement(new SampleExtz("hello", "dolly"));
        testExternalizable(new ExtWrapListPoly(a), new ExtWrapListPoly(), pf);
        testExternalizable(new ExtWrapTagged(new ExtWrapListPoly(a)), new ExtWrapTagged(), pf);
        //polymorphic vector with complex sub-types
        a.addElement(new ExtWrapList(y, new ExtWrapList(Integer.class))); //note: must manually wrap children in polymorphic lists
        testExternalizable(new ExtWrapListPoly(a), new ExtWrapListPoly(), pf);
        testExternalizable(new ExtWrapListPoly(new Vector()), new ExtWrapListPoly());

        //hashtables
        OrderedHashtable oh = new OrderedHashtable();
        testExternalizable(new ExtWrapMap(oh), new ExtWrapMap(String.class, Integer.class, ExtWrapMap.TYPE_ORDERED));
        testExternalizable(new ExtWrapMapPoly(oh), new ExtWrapMapPoly(Date.class, true));
        testExternalizable(new ExtWrapTagged(new ExtWrapMap(oh)), new ExtWrapTagged());
        testExternalizable(new ExtWrapTagged(new ExtWrapMapPoly(oh)), new ExtWrapTagged());
        oh.put("key1", new SampleExtz("a", "b"));
        oh.put("key2", new SampleExtz("c", "d"));
        oh.put("key3", new SampleExtz("e", "f"));
        testExternalizable(new ExtWrapMap(oh), new ExtWrapMap(String.class, SampleExtz.class, ExtWrapMap.TYPE_ORDERED), pf);
        testExternalizable(new ExtWrapTagged(new ExtWrapMap(oh)), new ExtWrapTagged(), pf);

        Hashtable h = new Hashtable();
        testExternalizable(new ExtWrapMap(h), new ExtWrapMap(String.class, Integer.class));
        testExternalizable(new ExtWrapMapPoly(h), new ExtWrapMapPoly(Date.class));
        testExternalizable(new ExtWrapTagged(new ExtWrapMap(h)), new ExtWrapTagged());
        testExternalizable(new ExtWrapTagged(new ExtWrapMapPoly(h)), new ExtWrapTagged());
        h.put("key1", new SampleExtz("e", "f"));
        h.put("key2", new SampleExtz("c", "d"));
        h.put("key3", new SampleExtz("a", "b"));
        testExternalizable(new ExtWrapMap(h), new ExtWrapMap(String.class, SampleExtz.class), pf);
        testExternalizable(new ExtWrapTagged(new ExtWrapMap(h)), new ExtWrapTagged(), pf);

        Hashtable j = new Hashtable();
        j.put(new Integer(17), h);
        j.put(new Integer(-3), h);
        Hashtable k = new Hashtable();
        k.put("key", j);
        testExternalizable(new ExtWrapMap(k, new ExtWrapMap(Integer.class, new ExtWrapMap(String.class, SampleExtz.class))),
                new ExtWrapMap(String.class, new ExtWrapMap(Integer.class, new ExtWrapMap(String.class, SampleExtz.class))), pf);    //note: this example contains mixed hashtable types; would choke if we used a tagging wrapper

        OrderedHashtable m = new OrderedHashtable();
        m.put("a", "b");
        m.put("b", new Integer(17));
        m.put("c", new Short((short)-443));
        m.put("d", new SampleExtz("boris", "yeltsin"));
        m.put("e", new ExtWrapList(vs));
        testExternalizable(new ExtWrapMapPoly(m), new ExtWrapMapPoly(String.class, true), pf);
    }

    /**
     * Test string serialization extension that handles large strings.
     */
    @Test
    public void stringExtSizeTest() throws IOException, DeserializationException {
        String largeString = buildLargeString();
        PrototypeFactory pf = new PrototypeFactory();
        PrototypeFactory.setStaticHasher(new ClassNameHasher());

        // serialize large string using new string serialization method
        byte[] serializedBytes = serializeStringToBytes(largeString);

        // deserialize the large string
        DataInputStream newInputStream = new DataInputStream(new ByteArrayInputStream(serializedBytes));
        String result = (String)ExtUtil.read(newInputStream, new ExtWrapTagged(), pf);
        newInputStream.close();

        // check that deserializing a large string with 'readString' fails
        boolean didFail = false;
        newInputStream = new DataInputStream(new ByteArrayInputStream(serializedBytes));
        try {
            ExtUtil.readString(newInputStream);
        } catch (Exception e) {
            didFail = true;
        }
        assertTrue("Deserializing a large string using 'readString' shouldn't wor", didFail);

        // test equality of original string with deserialized one
        assertEquals('a', result.charAt(0));
        assertEquals('z', result.charAt(result.length() - 1));
        assertEquals(largeString.length(), result.length());

        // assert that the string would have thrown an error using the old serialization implementation
        DataOutputStream dataOutputStream = new DataOutputStream(new ByteArrayOutputStream());
        didFail = false;
        try {
            writeStringOld(dataOutputStream, largeString);
        } catch (IOException e) {
            didFail = true;
        }
        assertTrue("The old string serialization method should fail on large strings", didFail);
    }

    private static String buildLargeString() {
        StringBuilder sb = new StringBuilder();
        sb.append("a");
        for (int i = 0; i < ((ExtUtil.MAX_UNSIGNED_SHORT_VALUE / 4) + 100); i++) {
            // 🚩 is at least 4 bytes long, more depending on encoding stuff...
            sb.append("🚩");
        }
        sb.append("z");
        return sb.toString();
    }

    private static byte[] serializeStringToBytes(String string) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(baos);
        try {
            ExtUtil.writeString(dataOutputStream, string);
            return baos.toByteArray();
        } catch (Exception e) {
            fail("shouldn't crash serializing large string");
        } finally {
            try {
                dataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return null;
    }

    /**
     * Check that serializing a small string defaults to the old implementation
     */
    @Test
    public void stringExtImplementationEqTest() throws IOException {
        PrototypeFactory.setStaticHasher(new ClassNameHasher());

        String message = "hello world!";
        // serialize and deserialize a small string using 'writeString' / 'readString'
        byte [] serializedString = serializeStringToBytes(message);

        DataInputStream newInputStream = new DataInputStream(new ByteArrayInputStream(serializedString));
        String deserializedString = ExtUtil.readString(newInputStream);
        assertEquals(message, deserializedString);

        // check that the old 'writeString' implementation matches the updated
        // 'writeString' implementation for small strings
        ByteArrayOutputStream oldByteStream = new ByteArrayOutputStream();
        DataOutputStream oldImplStream = new DataOutputStream(oldByteStream);
        writeStringOld(oldImplStream, message);

        assertNotNull(serializedString);
        assertEquals(oldByteStream.toString(), new String(serializedString));

        oldImplStream.close();
    }

    private static void writeStringOld(DataOutputStream out, String val) throws IOException {
        out.writeUTF(val);
    }
}
