/**
 *
 */
package org.javarosa.engine;

import java.util.Hashtable;

import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;

/**
 * @author ctsims
 *
 */
public class MockupProviderFactory extends InstanceInitializationFactory {
    Hashtable<String, FormInstance> instances;

    public MockupProviderFactory(Hashtable<String, FormInstance> instances) {
        this.instances = instances;
    }

    public AbstractTreeElement generateRoot(ExternalDataInstance instance) {
        String ref = instance.getReference();

        if(instances.containsKey(ref)) {
            FormInstance stored = instances.get(ref);

            TreeElement root = stored.getRoot();
            root.setInstanceName(instance.getInstanceId());

            root.setParent(instance.getBase());

            return root;
        } else if(ref.equals("jr://session")) {
            throw new IllegalArgumentException("Session instances not yet supported");
        } else {
            throw new IllegalArgumentException("There is no instance data registered in this mockup to: " + ref);
        }

    }

}
