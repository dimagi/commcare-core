/**
 *
 */
package org.commcare.resources.model;

/**
 * Resource Initialization Exceptions are thrown when there
 * is a problem with an installed resource initializing itself
 * locally.
 *
 * @author ctsims
 */
public class ResourceInitializationException extends Exception {

    public ResourceInitializationException(String message) {
        super(message);
    }
}
