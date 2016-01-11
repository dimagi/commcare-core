package org.javarosa.core.model.util.restorable;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.storage.Persistable;

import java.util.Date;
import java.util.Vector;

public class RestoreUtils {
    public static final String RECORD_ID_TAG = "rec-id";

    public static IXFormyFactory xfFact;

    private static TreeReference ref(String refStr) {
        return xfFact.ref(refStr);
    }

    private static TreeReference topRef(FormInstance dm) {
        return ref("/" + dm.getRoot().getName());
    }

    private static TreeReference childRef(String childPath, TreeReference parentRef) {
        return ref(childPath).parent(parentRef);
    }

<<<<<<< HEAD
=======
    private static FormInstance newDataModel(String topTag) {
        FormInstance dm = new FormInstance();
        dm.addNode(ref("/" + topTag));
        return dm;
    }

    public static FormInstance createDataModel(Restorable r) {
        FormInstance dm = newDataModel(r.getRestorableType());

        if (r instanceof Persistable) {
            addData(dm, RECORD_ID_TAG, new Integer(((Persistable)r).getID()));
        }

        return dm;
    }

    public static void addData(FormInstance dm, String xpath, Object data) {
        addData(dm, xpath, data, getDataType(data));
    }

    public static void addData(FormInstance dm, String xpath, Object data, int dataType) {
        if (data == null) {
            dataType = -1;
        }

        IAnswerData val;
        switch (dataType) {
            case -1:
                val = null;
                break;
            case Constants.DATATYPE_TEXT:
                val = new StringData((String)data);
                break;
            case Constants.DATATYPE_INTEGER:
                val = new IntegerData((Integer)data);
                break;
            case Constants.DATATYPE_LONG:
                val = new LongData((Long)data);
                break;
            case Constants.DATATYPE_DECIMAL:
                val = new DecimalData((Double)data);
                break;
            case Constants.DATATYPE_BOOLEAN:
                val = new StringData(((Boolean)data).booleanValue() ? "t" : "f");
                break;
            case Constants.DATATYPE_DATE:
                val = new DateData((Date)data);
                break;
            case Constants.DATATYPE_DATE_TIME:
                val = new DateTimeData((Date)data);
                break;
            case Constants.DATATYPE_TIME:
                val = new TimeData((Date)data);
                break;
            case Constants.DATATYPE_CHOICE_LIST:
                val = (SelectMultiData)data;
                break;
            default:
                throw new IllegalArgumentException("Don't know how to handle data type [" + dataType + "]");
        }

        TreeReference ref = absRef(xpath, dm);
        if (dm.addNode(ref, val, dataType) == null) {
            throw new RuntimeException("error setting value during object backup [" + xpath + "]");
        }
    }

    //used for outgoing data
    public static int getDataType(Object o) {
        int dataType = -1;
        if (o instanceof String) {
            dataType = Constants.DATATYPE_TEXT;
        } else if (o instanceof Integer) {
            dataType = Constants.DATATYPE_INTEGER;
        } else if (o instanceof Long) {
            dataType = Constants.DATATYPE_LONG;
        } else if (o instanceof Float || o instanceof Double) {
            dataType = Constants.DATATYPE_DECIMAL;
        } else if (o instanceof Date) {
            dataType = Constants.DATATYPE_DATE;
        } else if (o instanceof Boolean) {
            dataType = Constants.DATATYPE_BOOLEAN; //booleans are serialized as a literal 't'/'f'
        } else if (o instanceof SelectMultiData) {
            dataType = Constants.DATATYPE_CHOICE_LIST;
        }
        return dataType;
    }

>>>>>>> master
    //used for incoming data
    private static int getDataType(Class c) {
        int dataType;
        if (c == String.class) {
            dataType = Constants.DATATYPE_TEXT;
        } else if (c == Integer.class) {
            dataType = Constants.DATATYPE_INTEGER;
        } else if (c == Long.class) {
            dataType = Constants.DATATYPE_LONG;
        } else if (c == Float.class || c == Double.class) {
            dataType = Constants.DATATYPE_DECIMAL;
        } else if (c == Date.class) {
            dataType = Constants.DATATYPE_DATE;
            //Clayton Sims - Jun 16, 2009 - How are we handling Date v. Time v. DateTime?
        } else if (c == Boolean.class) {
            dataType = Constants.DATATYPE_TEXT; //booleans are serialized as a literal 't'/'f'
        } else {
            throw new RuntimeException("Can't handle data type " + c.getName());
        }

        return dataType;
    }

    public static Object getValue(String xpath, FormInstance tree) {
        TreeReference context = topRef(tree);
        TreeElement node = tree.resolveReference(ref(xpath).contextualize(context));
        if (node == null) {
            throw new RuntimeException("Could not find node [" + xpath + "] when parsing saved instance!");
        }

        if (node.isRelevant()) {
            IAnswerData val = node.getValue();
            return (val == null ? null : val.getValue());
        } else {
            return null;
        }
    }

    public static void applyDataType(FormInstance dm, String path, TreeReference parent, Class type) {
        applyDataType(dm, path, parent, getDataType(type));
    }

    public static void applyDataType(FormInstance dm, String path, TreeReference parent, int dataType) {
        TreeReference ref = childRef(path, parent);

        Vector v = new EvaluationContext(dm).expandReference(ref);
        for (int i = 0; i < v.size(); i++) {
            TreeElement e = dm.resolveReference((TreeReference)v.elementAt(i));
            e.setDataType(dataType);
        }
    }

    public static void templateData(Restorable r, FormInstance dm, TreeReference parent) {
        if (parent == null) {
            parent = topRef(dm);
            applyDataType(dm, "timestamp", parent, Date.class);
        }

        if (r instanceof Persistable) {
            applyDataType(dm, RECORD_ID_TAG, parent, Integer.class);
        }

        r.templateData(dm, parent);
    }
}
