package org.commcare.core.graph.c3;

<<<<<<< HEAD
=======
import org.commcare.core.graph.model.GraphData;
>>>>>>> master
import org.json.JSONException;

/**
 * Legend-related configuration for C3.
 *
 * Created by jschweers on 11/16/2015.
 */
public class LegendConfiguration extends Configuration {
<<<<<<< HEAD
    public LegendConfiguration(org.commcare.core.graph.model.GraphData data) throws JSONException {
=======
    public LegendConfiguration(GraphData data) throws JSONException {
>>>>>>> master
        super(data);

        // Respect user's preference for showing legend
        boolean showLegend = Boolean.valueOf(mData.getConfiguration("show-legend", "false"));
        if (!showLegend) {
            mConfiguration.put("show", false);
        }
    }
}
