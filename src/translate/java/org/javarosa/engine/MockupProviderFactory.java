/**
 *
 */
package org.javarosa.engine;

import org.javarosa.core.model.instance.ConcreteInstanceRoot;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.InstanceRoot;
import org.javarosa.core.model.instance.TreeElement;

import java.util.Hashtable;

/**
 * @author ctsims
 *
 */
public class MockupProviderFactory extends InstanceInitializationFactory {
    final Hashtable<String, FormInstance> instances;

    public MockupProviderFactory(Hashtable<String, FormInstance> instances) {
        this.instances = instances;
    }

    @Override
    public ExternalDataInstance getSpecializedExternalDataInstance(ExternalDataInstance instance) {
        return instance;
    }

    @Override
    public InstanceRoot generateRoot(ExternalDataInstance instance) {
        String ref = instance.getReference();

        if(instances.containsKey(ref)) {
            FormInstance stored = instances.get(ref);

            TreeElement root = stored.getRoot();
            root.setInstanceName(instance.getInstanceId());

            root.setParent(instance.getBase());

            return new ConcreteInstanceRoot(root);
        } else if(ref.equals("jr://session")) {
            throw new IllegalArgumentException("Session instances not yet supported");
        } else {
            throw new IllegalArgumentException("There is no instance data registered in this mockup to: " + ref);
        }

    }

}
