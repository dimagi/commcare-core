package org.commcare.suite.model;

import org.javarosa.core.util.ArrayUtilities;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * Defines top level UI logic for a case-select or case-detail view,
 * Part of {@code Detail} model
 */
public class Global implements Externalizable {

    private GeoOverlay[] geoOverlays;

    /**
     * Serialization Only
     */
    public Global() {
    }

    public Global(Vector<GeoOverlay> geoOverlays) {
        this.geoOverlays = ArrayUtilities.copyIntoArray(geoOverlays, new GeoOverlay[geoOverlays.size()]);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        Vector<GeoOverlay> theGeoOverlays = (Vector<GeoOverlay>)ExtUtil.read(in, new ExtWrapList(GeoOverlay.class), pf);
        geoOverlays = new GeoOverlay[theGeoOverlays.size()];
        ArrayUtilities.copyIntoArray(theGeoOverlays, geoOverlays);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapList(ArrayUtilities.toVector(geoOverlays)));
    }

    public GeoOverlay[] getGeoOverlays() {
        return geoOverlays;
    }
}
