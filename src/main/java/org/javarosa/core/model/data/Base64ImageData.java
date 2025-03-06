package org.javarosa.core.model.data;

import org.commcare.modern.util.Pair;
import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A response to a micro widget question, it returns a Pair of Strings, with the first storing the name of the file
 * and the second, a Base64 encoded image
 *
 * @author avazirna
 */
public class Base64ImageData implements IAnswerData {
    private Pair<String, String> base64EncodedImage;

    /**
     * Empty Constructor, necessary for dynamic construction during deserialization.
     * Shouldn't be used otherwise.
     */
    public Base64ImageData() {}
    public Base64ImageData(Pair<String, String> s) {
        setValue(s);
    }

    @Override
    public IAnswerData clone() {
        return new Base64ImageData(base64EncodedImage);
    }

    @Override
    public void setValue(Object o) {
        if (o == null) {
            throw new NullPointerException("Attempt to set an IAnswerData class to null.");
        }
        base64EncodedImage = (Pair<String, String>)o;
    }

    @Override
    public Pair<String, String> getValue() {
        return base64EncodedImage;
    }

    @Override
    public String getDisplayText() {
        return base64EncodedImage.first;
    }

    public String getFileName() { return base64EncodedImage.first;}

    public String getImageData() { return base64EncodedImage.second;}

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        base64EncodedImage = new Pair<>(ExtUtil.readString(in), ExtUtil.readString(in));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, base64EncodedImage.first);
        ExtUtil.writeString(out, base64EncodedImage.second);
    }

    @Override
    public UncastData uncast() {
        return new UncastData(toString());
    }

    @Override
    public Base64ImageData cast(UncastData data) throws IllegalArgumentException {
        String[] elements = DataUtil.splitOnSpaces(data.value);
        if (elements.length != 2) {
            throw new IllegalArgumentException("Two elements are expected, found "+ elements.length);
        }

        if (DataUtil.isImageFile(getFileName())){
            throw new IllegalArgumentException("First element needs to be the name of an image file, found "+ getFileName());
        }
        return new Base64ImageData(new Pair<>(elements[0], elements[1]));
    }

    @Override
    public String toString() {
        return base64EncodedImage.first + " " + base64EncodedImage.second;
    }
}
