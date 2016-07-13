package org.commcare.cases.util;

import org.commcare.cases.model.Case;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.util.MD5;

/**
 * @author ctsims
 */
public class CaseDBUtils {

    public static String computeCaseDbHash(IStorageUtility<Case> storage) {
        byte[] data = new byte[MD5.length];
        for (int i = 0; i < data.length; ++i) {
            data[i] = 0;
        }
        boolean casesExist = false;
        for (IStorageIterator<Case> i = storage.iterate(); i.hasMore(); ) {
            Case c = i.nextRecord();
            String record = c.getCaseId();
            byte[] current = MD5.hash(record.getBytes());
            data = xordata(data, current);
            casesExist = true;
        }

        //In the base case (with no cases), the case hash is empty
        if (!casesExist) {
            return "";
        }
        return MD5.toHex(data);
    }

    private static byte[] xordata(byte[] one, byte[] two) {
        if (one.length != two.length) {
            //Pad?
            throw new RuntimeException("Invalid XOR operation between byte arrays of unequal length");
        }
        byte[] output = new byte[one.length];
        for (int i = 0; i < one.length; ++i) {
            output[i] = (byte)(one[i] ^ two[i]);
        }

        return output;
    }

}
