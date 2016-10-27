package org.commcare.resources.model;

import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>A resource location is a simple model containing a possible
 * location for a resource's definition.</p>
 *
 * <p>Resource locations provide a URI (possibly a relative URI)
 * along with an authority for location.</p>
 *
 * @author ctsims
 */
public class ResourceLocation implements Externalizable {
    private int authority;
    private String location;
    private boolean relative;

    /**
     * For serialization only
     */
    public ResourceLocation() {
    }

    /**
     * @param authority The enumerated value defining the authority
     *                  associated with this location.
     * @param location  A URI (possibly relative) defining the location
     *                  of a resource's definition.
     */
    public ResourceLocation(int authority, String location) {
        this.authority = authority;
        this.location = location;
        this.relative = ReferenceManager.isRelative(location);
    }

    /**
     * @return The enumerated value defining the authority associated
     * with this location.
     */
    public int getAuthority() {
        return authority;
    }

    /**
     * @return A URI (possibly relative) defining the location
     * of a resource's definition.
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return Whether or not this location is a relative.
     */
    public boolean isRelative() {
        return relative;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        this.authority = ExtUtil.readInt(in);
        this.location = ExtUtil.readString(in);
        this.relative = ReferenceManager.isRelative(location);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, authority);
        ExtUtil.writeString(out, location);
        this.relative = ReferenceManager.isRelative(location);
    }
}
