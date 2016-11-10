package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A display unit element contains text and a set of potential image/audio
 * references for menus or other UI elements
 *
 * @author ctsims
 */
public class DisplayUnit implements Externalizable, DetailTemplate {

    private Text name;
    private Text imageReference;
    private Text audioReference;

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

    public DisplayData evaluate() {
        return evaluate(null);
    }

    @Override
    public DisplayData evaluate(EvaluationContext ec) {
        String imageRef = imageReference == null ? null : imageReference.evaluate(ec);
        String audioRef = audioReference == null ? null : audioReference.evaluate(ec);
        return new DisplayData(name.evaluate(ec),
                imageRef,
                audioRef);
    }

    /**
     * @return A Text which should be displayed to the user as
     * the action which will display this menu.
     */
    public Text getText() {
        return name;
    }

    public Text getImageURI() {
        return imageReference;
    }

    public Text getAudioURI() {
        return audioReference;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        name = (Text)ExtUtil.read(in, Text.class, pf);
        imageReference = (Text)ExtUtil.read(in, new ExtWrapNullable(Text.class), pf);
        audioReference = (Text)ExtUtil.read(in, new ExtWrapNullable(Text.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, name);
        ExtUtil.write(out, new ExtWrapNullable(imageReference));
        ExtUtil.write(out, new ExtWrapNullable(audioReference));
    }

}
