package org.javarosa.core.model.instance;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public interface DataInstanceBuilder<T extends ExternalDataInstance> {
   T buildDataInstance(String reference, String instanceid);
}
