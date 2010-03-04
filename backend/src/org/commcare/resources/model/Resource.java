/**
 * 
 */
package org.commcare.resources.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class Resource implements Persistable, IMetaData {
	
	public static final String META_INDEX_RESOURCE_ID = "ID";
	public static final String META_INDEX_RESOURCE_GUID = "RGUID";
	public static final String META_INDEX_PARENT_GUID = "PGUID";
	public static final String META_INDEX_VERSION = "PGUID";
	
	public static final int RESOURCE_AUTHORITY_LOCAL = 0;
	public static final int RESOURCE_AUTHORITY_REMOTE = 1;
	public static final int RESOURCE_AUTHORITY_CACHE = 2;
	public static final int RESOURCE_AUTHORITY_RELATIVE = 4;
	public static final int RESOURCE_AUTHORITY_TEMPORARY = 8;
	
	public static final int RESOURCE_STATUS_UNINITIALIZED = 0;
	public static final int RESOURCE_STATUS_LOCAL = 1;
	public static final int RESOURCE_STATUS_REMOTE = 2;
	public static final int RESOURCE_STATUS_INSTALLED = 4;
	public static final int RESOURCE_STATUS_UPGRADE = 8;
	public static final int RESOURCE_STATUS_DELETE = 16;
	public static final int RESOURCE_STATUS_OBSELETE = 32;
	
	private int recordId = -1;
	private int version;
	private int status;
	private String id;
	private Vector<ResourceLocation> locations;
	private ResourceInstaller initializer;
	private String guid;
	
	//Not sure if we want this persisted just yet...
	private String parent;
	
	/**
	 * For serialization only 
	 */
	public Resource() {
		
	}
	
	public Resource(int version, String id, Vector<ResourceLocation> locations) {
		this.version = version;
		this.id = id;
		this.locations = locations;
		this.guid = PropertyUtils.genGUID(25);
		this.status = RESOURCE_STATUS_UNINITIALIZED;
	}
	
	public InputStream OpenStream() {
		return null;
	}
	
	/**
	 * TODO: It is in the air whether this should be an operation
	 * which uses a copy, rather than the master list.
	 * @return
	 */
	public Vector<ResourceLocation> getLocations() {
		return locations;
	}
	
	public int getStatus() {
		return status;
	}
	
	public String getResourceId() {
		return id;
	}
	
	public String getRecordGuid() {
		return guid;
	}
	
	public void setParentId(String parent) {
		this.parent = parent;
	}
	
	public boolean hasParent() {
		if(parent == null || "".equals(parent)) {
			return false;
		} else{
			return true;
		}
	}
	
	public String getParentId() {
		return parent;
	}
	
	public int getVersion() {
		return version;
	}
	
	public void setInstaller(ResourceInstaller initializer) {
		this.initializer = initializer;
	}
	
	public ResourceInstaller getInstaller() {
		return initializer;
	}
	public void setStatus(int status) {
		this.status = status;
	}

	public int getID() {
		return recordId;
	}

	public void setID(int ID) {
		recordId = ID;
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		this.recordId = ExtUtil.readInt(in);
		this.version = ExtUtil.readInt(in);
		this.id = ExtUtil.readString(in);
		this.guid = ExtUtil.readString(in);
		this.status = ExtUtil.readInt(in);
		this.parent = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		
		locations = (Vector<ResourceLocation>)ExtUtil.read(in, new ExtWrapList(ResourceLocation.class),pf);
		this.initializer = (ResourceInstaller)ExtUtil.read(in, new ExtWrapTagged(), pf);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out,recordId);
		ExtUtil.writeNumeric(out,version);
		ExtUtil.writeString(out, id);
		ExtUtil.writeString(out,guid);
		ExtUtil.writeNumeric(out,status);
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(parent));
		
		ExtUtil.write(out, new ExtWrapList(locations));
		ExtUtil.write(out, new ExtWrapTagged(initializer));
	}

	public Hashtable getMetaData() {
		Hashtable md = new Hashtable();
		String[] fields = getMetaDataFields();
		for(int i = 0 ; i < fields.length ; ++i ) {
			md.put(fields[i],getMetaData(fields[i]));
		}
		return md;
	}

	public Object getMetaData(String fieldName) {
		if(fieldName.equals(META_INDEX_RESOURCE_ID)) {
			return id;
		} else  if(fieldName.equals(META_INDEX_RESOURCE_GUID)) {
			return guid;
		} else if(fieldName.equals(META_INDEX_PARENT_GUID)) {
			return parent == null ? "" : parent;
		} else if(fieldName.equals(META_INDEX_VERSION)) {
			return new Integer(version);
		}
		throw new IllegalArgumentException("No Field w/name " + fieldName + " is relevant for resources");
	}

	public String[] getMetaDataFields() {
		return new String[] {META_INDEX_RESOURCE_ID,META_INDEX_RESOURCE_GUID, META_INDEX_PARENT_GUID,META_INDEX_VERSION};
	}
}
