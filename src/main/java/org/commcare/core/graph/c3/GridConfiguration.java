package org.commcare.core.graph.c3;

<<<<<<< HEAD
=======
import org.commcare.core.graph.model.GraphData;
>>>>>>> master
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Grid-related configuration for C3.
 *
 * Created by jschweers on 11/16/2015.
 */
public class GridConfiguration extends Configuration {
<<<<<<< HEAD
    public GridConfiguration(org.commcare.core.graph.model.GraphData data) throws JSONException {
=======
    public GridConfiguration(GraphData data) throws JSONException {
>>>>>>> master
        super(data);

        boolean showGrid = Boolean.valueOf(mData.getConfiguration("show-grid", "true"));
        if (showGrid) {
            JSONObject show = new JSONObject("{ show: true }");
            mConfiguration.put("x", show);
            mConfiguration.put("y", show);
        }
    }
}
