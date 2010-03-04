package org.commcare.resources.model;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.reference.InvalidReferenceException;
import org.commcare.reference.Reference;
import org.commcare.reference.ReferenceUtil;

public class ResourceTable {

	private Hashtable<String, Resource> resources;

	// private IStorageUtilityIndexed<Resource> resources;

	/**
	 * For Serialization Only!
	 */
	public ResourceTable() {

	}

	public static ResourceTable RetrieveGlobalResourceTable() {
		return null;
	}

	public static ResourceTable CreateTemporaryResourceTable() {
		ResourceTable table = new ResourceTable();
		table.resources = new Hashtable<String, Resource>();
		return table;
	}

	public void addResource(Resource resource) {
		this.addResource(resource, new BasicResourceInitializer(), "");
	}

	public void removeResource(Resource resource) {
		this.resources.remove(resource.getResourceId());
	}

	public void addResource(Resource resource, ResourceInstaller initializer, String parentId) {
		resource.setInitializer(initializer);
		resource.setParentId(parentId);
		resources.put(resource.getResourceId(), resource);
	}
	
	public Vector<Resource> getResourcesForParent(String parent) {
		Vector<Resource> v = new Vector<Resource>();
		for (Enumeration en = resources.elements(); en.hasMoreElements();) {
			Resource r = (Resource) en.nextElement();
			if(parent.equals(r.getParentId())) {
				v.addElement(r);
			}
		}
		return v;
	}

	public Resource getResourceWithId(String id) {
		return resources.get(id);
	}
	
	public Resource getResourceWithGuid(String guid) {
		for (Enumeration en = resources.elements(); en.hasMoreElements();) {
			Resource r = (Resource) en.nextElement();
			if(r.getRecordGuid().equals(guid)) {
				return r;
			}
		}
		return null;
	}


	private Vector<Resource> GetResources() {
		Vector<Resource> v = new Vector<Resource>();
		for (Enumeration en = resources.elements(); en.hasMoreElements();) {
			v.addElement((Resource) en.nextElement());
		}
		return v;
	}
	
	private Vector<Resource> GetResources(int status) {
		Vector<Resource> v = new Vector<Resource>();
		for (Enumeration en = resources.elements(); en.hasMoreElements();) {
			Resource r = (Resource) en.nextElement();
			if(r.getStatus() == status) {
				v.addElement(r);
			}
		}
		return v;
	}

	private Vector<Resource> GetUnreadyResources() {
		Vector<Resource> v = new Vector<Resource>();
		for (Enumeration en = resources.elements(); en.hasMoreElements();) {
			Resource r = (Resource) en.nextElement();
			if (r.getStatus() != Resource.RESOURCE_STATUS_INSTALLED
					&& r.getStatus() != Resource.RESOURCE_STATUS_UPGRADE) {
				v.addElement(r);
			}
		}
		return v;
	}

	public boolean isReady() {
		if (GetUnreadyResources().size() > 0) {
			return false;
		} else {
			return true;
		}
	}

	public void prepareResources(ResourceTable master) throws UnresolvedResourceException {
		Vector<Resource> v = GetUnreadyResources();
		int round = -1;
		while (v.size() > 0) {
			round++;
			System.out.println("Preparing resources round " + round + ". " + v.size() + " resources remain");
			for (Resource r : v) {
				boolean upgrade = false;
				//Make a reference set for all invalid references (this will get filled in for us)
				Vector<Reference> invalid = new Vector<Reference>();
				
				//All operations regarding peers and master table
				if (master != null) {
					Resource peer = master.getResourceWithId(r.getResourceId());
				
					if (peer != null) {
						//TODO: For now we're assuming that Versions greater than the 
						//current are always acceptable
						if (peer.getVersion() >= r.getVersion()) {
							// This resource is already up to date in the master. Set 
							// its status to installed already.
							r.setStatus(Resource.RESOURCE_STATUS_INSTALLED);
							continue;
						} else {
							upgrade = true;
						}
						//TODO: This might not be worth the time it takes to pre-calculate.
						//Should consider moving this to after it is determined whether
						//local references exist;
						invalid = ResourceTable.explodeLocalReferences(peer, master);
					}
				}
				
				// Vector<Reference> refs = explodeAllReferences(r, this,
				// master);

				boolean handled = false;

				for (ResourceLocation location : r.getLocations()) {
					if(handled) {
						break;
					}
					if (location.isRelative()) {
						for (Reference ref : explodeReferences(location, r,this, master)) {
							if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_LOCAL && invalid.contains(ref)) {
								// Nothing
							} else {
								handled = r.getInitializer().install(r, location, ref, this, upgrade);
								if(handled) {
									break;
								}
							}
						}
					} else {
						try {
							handled = r.getInitializer().install(r, location, ReferenceUtil.DeriveReference(location.getLocation()), this, upgrade);
							if(handled) {
								break;
							}
						} catch(InvalidReferenceException ire) {
							ire.printStackTrace();
							//Continue until no resources can be found.
						}
					}
				}
				if(!handled) {
					throw new UnresolvedResourceException(r, "No external or local definition could be found for resource " + r.getResourceId()); 
				}

			}
			v = GetUnreadyResources();
		}
	}
	
	public boolean upgradeTable(ResourceTable incoming) {
		if(!incoming.isReady()) {
			return false;
		}
		
		for(Resource r : incoming.GetResources()) {
			Resource peer = this.getResourceWithId(r.getResourceId());
			if(peer == null) {
				this.addResource(r);
			} else {
				if(peer.getVersion() == r.getVersion()) {
					//Same resource. Don't do anything with it, it has no
					//children, so ID's don't need to change.
					//Technically resource locations could change, worth thinking
					//about for the future.
				}
				if(peer.getVersion() < r.getVersion()) {
					peer.getInitializer().uninstall(peer, this, incoming);
					if(r.getStatus() == Resource.RESOURCE_STATUS_INSTALLED) {
						this.addResource(r);
					} else if(r.getStatus() == Resource.RESOURCE_STATUS_UPGRADE) {
						r.getInitializer().upgrade(r);
						r.setStatus(Resource.RESOURCE_STATUS_INSTALLED);
						this.addResource(r);
					}
				}
			}
		}
		
		System.out.println(this);
		
		// All of the incoming resources should now be installed and ready to roll.
		// The only thing left to do is run a cleanup on this table to clear out any
		// irrelevant resources. 
		// It's important to note that there is technically something that could go wrong here.
		// If the incoming table is lost before this step is completed, future descendents
		// may not know whether their children are relevant. As such, the installation
		// cannot really be marked completed (and the incoming table deleted) until
		// all deletions are made. 
		Vector<Resource> pendingDelete = GetResources(Resource.RESOURCE_STATUS_DELETE);
		while(pendingDelete.size() > 0) {
			for(Resource r : pendingDelete) {
				//Delete pending resource, possibly marking further resources for deletion
				r.getInitializer().uninstall(r, this, incoming);
			}
			pendingDelete = GetResources(Resource.RESOURCE_STATUS_DELETE);
			System.out.println("After of pending deletes:");
			System.out.println(this);
		}
		
		return true;
	}
	
	public String toString() {
		String output = "";
		for(Resource r : GetResources()) {
			output += "| " + r.getResourceId() + " | " + r.getVersion() + " | " + getStatus(r.getStatus()) + " |\n";
		}
		return output;
	}
	
	public String getStatus(int status) {
		switch(status) {
		case Resource.RESOURCE_STATUS_UNINITIALIZED:
			return "Uninitialized";
		case Resource.RESOURCE_STATUS_LOCAL:
			return "Local";
		case Resource.RESOURCE_STATUS_REMOTE:
			return "Remote";
		case Resource.RESOURCE_STATUS_INSTALLED:
			return "Installed";
		case Resource.RESOURCE_STATUS_UPGRADE:
			return "Ready for Upgrade";
		case Resource.RESOURCE_STATUS_DELETE:
			return "Flagged for Deletion";
		default:
			return "Unknown";
		}
	}

	private static Vector<Reference> explodeLocalReferences(Resource r, ResourceTable t) {
		Vector<ResourceLocation> locations = r.getLocations();
		Vector<Reference> ret = new Vector<Reference>();
		for(ResourceLocation location : locations) {
			if(location.isRelative()) {
				if(r.hasParent()) {
					Resource parent = t.getResourceWithGuid(r.getParentId());
					if(parent != null) {
						//Get all local references for the parent
						Vector<Reference> parentRefs = explodeLocalReferences(parent, t);
						for(Reference context : parentRefs) {
							try{
								ret.addElement(ReferenceUtil.DeriveReference(context, location.getLocation()));
							}catch(InvalidReferenceException ire) {
								ire.printStackTrace();
							}
						}
					}
				}
			}
			else if(location.getAuthority() == Resource.RESOURCE_AUTHORITY_LOCAL) {
				try {
					ret.addElement(ReferenceUtil.DeriveReference(location.getLocation()));
				} catch (InvalidReferenceException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}
	
	private static Vector<Reference> explodeReferences(ResourceLocation location, Resource r, ResourceTable t, ResourceTable m) {
		int type = location.getAuthority();
		Vector<Reference> ret = new Vector<Reference>();
		if(r.hasParent()) {
			Resource parent = t.getResourceWithGuid(r.getParentId());
			
			//If the local table doesn't have the parent ref, try the master
			if(parent == null && m != null) {
				parent = m.getResourceWithGuid(r.getParentId());
			}
			if(parent != null) {
				//Get all local references for the parent
				Vector<Reference> parentRefs = explodeAllReferences(type, parent, t, m);
				for(Reference context : parentRefs) {
					try {
						ret.addElement(ReferenceUtil.DeriveReference(context, location.getLocation()));
					} catch (InvalidReferenceException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return ret;
	}
	
	private static Vector<Reference> explodeAllReferences(int type, Resource r, ResourceTable t, ResourceTable m) {
		Vector<ResourceLocation> locations = r.getLocations();
		Vector<Reference> ret = new Vector<Reference>();
		for(ResourceLocation location : locations) {
			if(location.getAuthority() == type) {
			 if(location.isRelative()) {
				if(r.hasParent()) {
					Resource parent = t.getResourceWithGuid(r.getParentId());
					
					//If the local table doesn't have the parent ref, try the master
					if(parent == null) {
						parent = m.getResourceWithGuid(r.getParentId());
					}
					if(parent != null) {
						//Get all local references for the parent
						Vector<Reference> parentRefs = explodeAllReferences(type, parent, t, m);
						for(Reference context : parentRefs) {
							try {
								ret.addElement(ReferenceUtil.DeriveReference(context, location.getLocation()));
							} catch (InvalidReferenceException e) {
								e.printStackTrace();
							}
						}
					}
				}
			} else  {
				try {
					ret.addElement(ReferenceUtil.DeriveReference(location.getLocation()));
				} catch (InvalidReferenceException e) {
					e.printStackTrace();
				}
			}
		}
		}
		return ret;
	}
	
}
