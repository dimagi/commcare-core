package org.javarosa.core.storage;

import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Basic model for storage
 *
 * Created by ctsims on 9/22/2017.
 */

public class Shoe implements Persistable, IMetaData {
    public static final String META_BRAND = "brand";
    public static final String META_SIZE = "size";
    public static final String META_STYLE = "style";

    String brand;
    String size;
    String style;

    int recordId = -1;
    private String reviewText = "";

    public Shoe() {

    }

    public Shoe(String brand, String style, String size) {
        if(brand == null || style == null || size == null) {
            throw new IllegalArgumentException("No values can be null");
        }
        this.brand = brand;
        this.size = size;
        this.style = style;
    }

    public void setReview(String reviewText) {
        if(reviewText == null) {
            reviewText = "";
        }
        this.reviewText = reviewText;
    }



    @Override
    public String[] getMetaDataFields() {
        return new String[] {META_BRAND, META_SIZE, META_STYLE};
    }

    @Override
    public Object getMetaData(String fieldName) {
        if(fieldName.equals(META_BRAND)) {
            return brand;
        } else if(fieldName.equals(META_SIZE)) {
            return size;
        } else if(fieldName.equals(META_STYLE)) {
            return style;
        }
        throw new IllegalArgumentException("No meta field: " + fieldName);
    }

    @Override
    public void setID(int ID) {
        this.recordId = ID;
    }

    @Override
    public int getID() {
        return this.recordId;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        brand = ExtUtil.readString(in);
        style = ExtUtil.readString(in);
        size = ExtUtil.readString(in);
        reviewText = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, brand);
        ExtUtil.writeString(out, style);
        ExtUtil.writeString(out, size);

        ExtUtil.writeString(out, reviewText);

    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Shoe)) {
            return false;
        }
        Shoe s = (Shoe)o;
        return this.size.equals(s.size) &&
                this.style.equals(s.style) &&
                this.brand.equals(s.brand) &&
                this.reviewText.equals(s.reviewText);
    }

    @Override
    public int hashCode() {
        return this.size.hashCode() ^
                this.style.hashCode() ^
                this.brand.hashCode() ^
                this.reviewText.hashCode();
    }

    public String getReviewText() {
        return reviewText;
    }
}
