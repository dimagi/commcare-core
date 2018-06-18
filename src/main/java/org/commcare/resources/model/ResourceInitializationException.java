package org.commcare.resources.model;

/**
 * Created by amstone326 on 5/30/18.
 */

public class ResourceInitializationException extends Exception {

    private Resource resource;

    public ResourceInitializationException(Resource r, Exception reason) {
        super(String.format(
                "Initialization failed for resource with id %s (%s) due to the following exception: %s",
                r.getResourceId(), r.getDescriptor(), reason.getMessage()));
        this.resource = r;
    }

    public Resource getResource() {
        return resource;
    }
}
