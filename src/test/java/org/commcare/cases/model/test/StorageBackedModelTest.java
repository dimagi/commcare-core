package org.commcare.cases.model.test;

import org.commcare.cases.model.StorageBackedModel;
import org.junit.Test;

import java.util.HashSet;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class StorageBackedModelTest {

    @Test
    public void modelPutAndGetTests() throws Exception {
        Hashtable<String, String> attributes = new Hashtable<>();
        attributes.put("id", "the_id");
        attributes.put("name", "sven");

        Hashtable<String, String> elements = new Hashtable<>();
        elements.put("age", "44");
        elements.put("id", "svens_national_id");

        StorageBackedModel model = new StorageBackedModel(attributes, elements);

        // check that DB column names created from attributes and elements
        // don't collide when the attributes and elements do
        HashSet<String> escapedAttrs = new HashSet<>(model.getEscapedAttributeKeys());
        escapedAttrs.retainAll(model.getEscapedElementKeys());
        assertTrue(escapedAttrs.isEmpty());

        // ensure that pulling attribute/element values out of model via
        // metadata lookup corresponds to actual data put into the model
        for (String attrKey : model.getEscapedAttributeKeys()) {
            assertEquals(attributes.get(StorageBackedModel.removeEscape(attrKey)),
                    model.getMetaData(attrKey));
        }

        for (String elemKey : model.getEscapedElementKeys()) {
            assertEquals(elements.get(StorageBackedModel.removeEscape(elemKey)),
                    model.getMetaData(elemKey));
        }
    }
}
