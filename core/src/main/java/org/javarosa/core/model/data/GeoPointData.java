package org.javarosa.core.model.data;

import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;


/**
 * A response to a question requesting an GeoPoint Value.
 *
 * @author Yaw Anokwa
 */
public class GeoPointData implements IAnswerData {

    // latitude, longitude, and potentially altitude and accuracy data
    private double[] gp = new double[4];
    private int len = 2;

    // accuracy and altitude data points stored will contain this many decimal
    // points:
    private final int MAX_DECIMAL_ACCURACY = 2;


    /**
     * Empty Constructor, necessary for dynamic construction during
     * deserialization. Shouldn't be used otherwise.
     */
    public GeoPointData() {

    }


    public GeoPointData(double[] gp) {
        this.fillArray(gp);
    }


    /**
     * Copy data in argument array into local geopoint array.
     *
     * @param gp double array of max size 4 representing geopoints
     */
    private void fillArray(double[] gp) {
        len = gp.length;
        for (int i = 0; i < len; i++) {
            if (i < 2) {
                // don't round lat & lng decimal values
                this.gp[i] = gp[i];
            } else {
                // accuracy & altitude should have their decimal values rounded
                this.gp[i] = roundDecimalUp(gp[i], MAX_DECIMAL_ACCURACY);
            }
        }
    }


    public IAnswerData clone() {
        return new GeoPointData(gp);
    }


    /*
     * (non-Javadoc)
     *
     * @see org.javarosa.core.model.data.IAnswerData#getDisplayText()
     */
    public String getDisplayText() {
        String s = "";
        for (int i = 0; i < len; i++) {
            s += gp[i] + " ";
        }
        return s.trim();

    }


    /*
     * (non-Javadoc)
     *
     * @see org.javarosa.core.model.data.IAnswerData#getValue()
     */
    public double[] getValue() {
        return gp;
    }

    public double getLatitude() { return gp[0]; }

    public double getLongitude() { return gp[1]; }


    public void setValue(Object o) {
        if (o == null) {
            throw new NullPointerException("Attempt to set an IAnswerData class to null.");
        }
        this.fillArray((double[])o);
    }


    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException,
            DeserializationException {
        len = (int)ExtUtil.readNumeric(in);
        for (int i = 0; i < len; i++) {
            gp[i] = ExtUtil.readDecimal(in);
        }
    }


    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, len);
        for (int i = 0; i < len; i++) {
            ExtUtil.writeDecimal(out, gp[i]);
        }
    }


    public UncastData uncast() {
        return new UncastData(getDisplayText());
    }

    public GeoPointData cast(UncastData data) throws IllegalArgumentException {
        double[] ret = new double[4];

        Vector<String> choices = DateUtils.split(data.value, " ", true);
        int i = 0;
        for (String s : choices) {
            double d = Double.parseDouble(s);
            ret[i] = d;
            ++i;
        }
        return new GeoPointData(ret);
    }

    /**
     * Jenky (but J2ME-compatible) decimal rounding (up) of doubles.
     *
     * Subject to normal double imprecisions and will encounter numerical
     * overflow problems if x * (10^numberofDecimals) is greater than
     * Double.MAX_VALUE or less than Double.MIN_VALUE.
     *
     * @param x                double to be rounded up
     * @param numberOfDecimals number of decimals that should present in result
     */
    private static double roundDecimalUp(double x, int numberOfDecimals) {
        int factor = (int)Double.parseDouble("1e" + numberOfDecimals);

        return Math.floor(x * factor) / factor;
    }
}
