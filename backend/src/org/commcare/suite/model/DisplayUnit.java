package org.commcare.suite.model;

import org.commcare.suite.model.graph.DisplayData;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>A display unit element contains text and a set of potential image/audio
 * references for menus or other UI elements</p>
 *
 * @author ctsims
 */
public class DisplayUnit implements Externalizable, DetailTemplate {

    Text name;
    Text imageReference;
    Text audioReference;

    /**
     * Serialization only!!!
     */
    public DisplayUnit() {

    }


    public DisplayUnit(Text name, Text imageReference, Text audioReference) {
        this.name = name;
        this.imageReference = imageReference;
        this.audioReference = audioReference;
    }

    public DisplayData evaluate(EvaluationContext ec){
        return new DisplayData(name.evaluate(ec),
                imageReference.evaluate(ec),
                audioReference.evaluate(ec));
    }

    /**
     * @return A Text which should be displayed to the user as
     * the action which will display this menu.
     */
    public Text getText() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        name = (Text)ExtUtil.read(in, Text.class, pf);
        imageReference = (Text)ExtUtil.read(in, Text.class, pf);
        audioReference = (Text)ExtUtil.read(in, Text.class, pf);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, name);
        ExtUtil.write(out, imageReference);
        ExtUtil.write(out, audioReference);
    }


    public Text getImageURI() {
        return imageReference;
    }

    public Text getAudioURI() {
        return audioReference;
    }
}
