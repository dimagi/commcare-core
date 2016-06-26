/**
 *
 */
package org.javarosa.engine.xml.serializer;

import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.engine.models.Mockup;
import org.javarosa.model.xform.DataModelSerializer;
import org.kxml2.io.KXmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * @author ctsims
 *
 */
public class MockupSerializer {
    final String XMLNS = "http://javarosa.org/mockup";

    final KXmlSerializer s;
    Mockup mockup;

    public MockupSerializer(OutputStream o, Mockup m) throws IOException {
        s = new KXmlSerializer();
        s.setOutput(o, "UTF-8");
        s.setPrefix("", XMLNS);
        this.mockup = m;
    }

    public void serialize() throws IOException {
        s.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        s.startDocument("UTF-8", null);
        s.startTag(XMLNS, "mockup");

        if(mockup.getDate() != null || mockup.getInstances() != null) {
            s.startTag(XMLNS, "context");
            if(mockup.getDate() != null){
                s.startTag(XMLNS, "date");
                s.text(DateUtils.formatDate(mockup.getDate(), DateUtils.FORMAT_ISO8601));
                s.endTag(XMLNS, "date");
            }

            Hashtable<String, FormInstance> instances = mockup.getInstances();
            for(Enumeration en = instances.keys() ; en.hasMoreElements() ;) {
                String key = (String)en.nextElement();
                DataInstance theInstance = instances.get(key);

                s.startTag(XMLNS, "instance");

                s.attribute(null, "src", key);

                DataModelSerializer dms = new DataModelSerializer(s);
                dms.serialize(theInstance, null);

                s.endTag(XMLNS, "instance");
            }
            s.endTag(XMLNS, "context");
        }


        s.endTag(XMLNS, "mockup");
        s.endDocument();
    }
}
