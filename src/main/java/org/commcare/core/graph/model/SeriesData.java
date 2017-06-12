package org.commcare.core.graph.model;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Contains the fully-evaluated data for a single graph series.
 *
 * @author jschweers
 */
public class SeriesData implements ConfigurableData {
<<<<<<< HEAD
    private final Vector<org.commcare.core.graph.model.XYPointData> mPoints;
=======
    private final Vector<XYPointData> mPoints;
>>>>>>> master
    private final Hashtable<String, String> mConfiguration;

    public SeriesData() {
        mPoints = new Vector<>();
        mConfiguration = new Hashtable<>();
    }

<<<<<<< HEAD
    public void addPoint(org.commcare.core.graph.model.XYPointData p) {
        mPoints.addElement(p);
    }

    public Vector<org.commcare.core.graph.model.XYPointData> getPoints() {
=======
    public void addPoint(XYPointData p) {
        mPoints.addElement(p);
    }

    public Vector<XYPointData> getPoints() {
>>>>>>> master
        return mPoints;
    }

    /**
     * Number of points in the series.
     */
    public int size() {
        return mPoints.size();
    }

    @Override
    public void setConfiguration(String key, String value) {
        mConfiguration.put(key, value);
    }

    @Override
    public String getConfiguration(String key) {
        return mConfiguration.get(key);
    }

    @Override
    public String getConfiguration(String key, String defaultValue) {
        String value = getConfiguration(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
