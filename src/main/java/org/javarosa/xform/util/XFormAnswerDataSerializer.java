/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.xform.util;

import org.commcare.modern.util.Pair;
import org.javarosa.core.data.IDataPointer;
import org.javarosa.core.model.IAnswerDataSerializer;
import org.javarosa.core.model.data.Base64ImageData;
import org.javarosa.core.model.data.BooleanData;
import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.DecimalData;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.LongData;
import org.javarosa.core.model.data.PointerAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.utils.DateUtils;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

/**
 * The XFormAnswerDataSerializer takes in AnswerData objects, and provides
 * an XForms compliant (String or Element) representation of that AnswerData.
 *
 * By default, this serializer can properly operate on StringData, DateData
 * SelectMultiData, and SelectOneData AnswerData objects. This list can be
 * extended by registering appropriate XForm serializing AnswerDataSerializers
 * with this class.
 *
 * @author Clayton Sims
 */
public class XFormAnswerDataSerializer implements IAnswerDataSerializer {

    public final static String DELIMITER = " ";

    final Vector additionalSerializers = new Vector();

    @Override
    public boolean canSerialize(IAnswerData data) {
        return data instanceof StringData || data instanceof DateData || data instanceof TimeData ||
                data instanceof SelectMultiData || data instanceof SelectOneData ||
                data instanceof IntegerData || data instanceof DecimalData || data instanceof PointerAnswerData ||
                data instanceof GeoPointData || data instanceof LongData || data instanceof DateTimeData || data instanceof UncastData
                || data instanceof Base64ImageData;
    }

    /**
     * @param data The AnswerDataObject to be serialized
     * @return A String which contains the given answer
     */
    public Object serializeAnswerData(UncastData data) {
        return data.getString();
    }


    /**
     * @param data The AnswerDataObject to be serialized
     * @return A String which contains the given answer
     */
    public Object serializeAnswerData(StringData data) {
        return data.getValue();
    }

    /**
     * @param data The AnswerDataObject to be serialized
     * @return A String which contains a date in xsd:date
     * formatting
     */
    public Object serializeAnswerData(DateData data) {
        return DateUtils.formatDate((Date)data.getValue(), DateUtils.FORMAT_ISO8601);
    }

    /**
     * @param data The AnswerDataObject to be serialized
     * @return A String which contains a date in xsd:date
     * formatting
     */
    public Object serializeAnswerData(DateTimeData data) {
        return DateUtils.formatDateTime((Date)data.getValue(), DateUtils.FORMAT_ISO8601);
    }

    /**
     * @param data The AnswerDataObject to be serialized
     * @return A String which contains a date in xsd:time
     * formatting
     */
    public Object serializeAnswerData(TimeData data) {
        return DateUtils.formatTime((Date)data.getValue(), DateUtils.FORMAT_ISO8601_WALL_TIME);
    }

    /**
     * @param data The AnswerDataObject to be serialized
     * @return A String which contains a reference to the
     * data
     */
    public Object serializeAnswerData(PointerAnswerData data) {
        //Note: In order to override this default behavior, a
        //new serializer should be used, and then registered
        //with this serializer
        IDataPointer pointer = (IDataPointer)data.getValue();
        return pointer.getDisplayText();
    }

    /**
     * @param data The AnswerDataObject to be serialized
     * @return A string containing the xforms compliant format
     * for a <select> tag, a string containing a list of answers
     * separated by space characters.
     */
    public Object serializeAnswerData(SelectMultiData data) {
        Vector selections = data.getValue();
        Enumeration en = selections.elements();
        StringBuffer selectString = new StringBuffer();

        while (en.hasMoreElements()) {
            Selection selection = (Selection)en.nextElement();
            if (selectString.length() > 0)
                selectString.append(DELIMITER);
            selectString.append(selection.getValue());
        }
        //As Crazy, and stupid, as it sounds, this is the XForms specification
        //for storing multiple selections.
        return selectString.toString();
    }

    /**
     * @param data The AnswerDataObject to be serialized
     * @return A String which contains the value of a selection
     */
    public Object serializeAnswerData(SelectOneData data) {
        return ((Selection)data.getValue()).getValue();
    }

    public Object serializeAnswerData(IntegerData data) {
        return data.getValue().toString();
    }

    public Object serializeAnswerData(LongData data) {
        return data.getValue().toString();
    }

    public Object serializeAnswerData(DecimalData data) {
        return data.getValue().toString();
    }

    public Object serializeAnswerData(GeoPointData data) {
        return data.getDisplayText();
    }

    public Object serializeAnswerData(Base64ImageData data) {
        Pair<String, String> base64ImageData = (Pair<String, String>) data.getValue();
        return base64ImageData.first + " " + base64ImageData.second;
    }

    public Object serializeAnswerData(BooleanData data) {
        if ((Boolean)data.getValue()) {
            return "1";
        } else {
            return "0";
        }
    }

    @Override
    public Object serializeAnswerData(IAnswerData data, int dataType) {
        // First, we want to go through the additional serializers, as they should
        // take priority to the default serializations
        Enumeration en = additionalSerializers.elements();
        while (en.hasMoreElements()) {
            IAnswerDataSerializer serializer = (IAnswerDataSerializer)en.nextElement();
            if (serializer.canSerialize(data)) {
                return serializer.serializeAnswerData(data, dataType);
            }
        }
        //Defaults
        return serializeAnswerData(data);
    }

    @Override
    public Object serializeAnswerData(IAnswerData data) {
        if (data instanceof StringData) {
            return serializeAnswerData((StringData)data);
        } else if (data instanceof SelectMultiData) {
            return serializeAnswerData((SelectMultiData)data);
        } else if (data instanceof SelectOneData) {
            return serializeAnswerData((SelectOneData)data);
        } else if (data instanceof IntegerData) {
            return serializeAnswerData((IntegerData)data);
        } else if (data instanceof LongData) {
            return serializeAnswerData((LongData)data);
        } else if (data instanceof DecimalData) {
            return serializeAnswerData((DecimalData)data);
        } else if (data instanceof DateData) {
            return serializeAnswerData((DateData)data);
        } else if (data instanceof TimeData) {
            return serializeAnswerData((TimeData)data);
        } else if (data instanceof PointerAnswerData) {
            return serializeAnswerData((PointerAnswerData)data);
        } else if (data instanceof GeoPointData) {
            return serializeAnswerData((GeoPointData)data);
        } else if (data instanceof DateTimeData) {
            return serializeAnswerData((DateTimeData)data);
        } else if (data instanceof BooleanData) {
            return serializeAnswerData((BooleanData)data);
        } else if (data instanceof UncastData) {
            return serializeAnswerData((UncastData)data);
        } else if (data instanceof Base64ImageData) {
            return serializeAnswerData((Base64ImageData)data);
        }

        return null;
    }

    @Override
    public Boolean containsExternalData(IAnswerData data) {
        //First check for registered serializers to identify whether
        //they override this one.
        Enumeration en = additionalSerializers.elements();
        while (en.hasMoreElements()) {
            IAnswerDataSerializer serializer = (IAnswerDataSerializer)en.nextElement();
            Boolean contains = serializer.containsExternalData(data);
            if (contains != null) {
                return contains;
            }
        }
        if (data instanceof PointerAnswerData) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public IDataPointer[] retrieveExternalDataPointer(IAnswerData data) {
        Enumeration en = additionalSerializers.elements();
        while (en.hasMoreElements()) {
            IAnswerDataSerializer serializer = (IAnswerDataSerializer)en.nextElement();
            Boolean contains = serializer.containsExternalData(data);
            if (contains != null) {
                return serializer.retrieveExternalDataPointer(data);
            }
        }
        if (data instanceof PointerAnswerData) {
            IDataPointer[] pointer = new IDataPointer[1];
            pointer[0] = (IDataPointer)data.getValue();
            return pointer;
        }
        //This shouldn't have been called.
        return null;
    }
}
