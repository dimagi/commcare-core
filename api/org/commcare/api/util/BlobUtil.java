package org.commcare.api.util;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by wpride1 on 6/16/15.
 */
public class BlobUtil {

    public static byte[] storeInDB(ArrayList<Long> longs) throws IOException, SQLException {

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(bout);
        for (long l : longs) {
            dout.writeLong(l);
        }
        dout.close();
        byte[] asBytes = bout.toByteArray();

        return asBytes;
    }

    public static ArrayList<Long> readFromDB() throws IOException, SQLException {

        ArrayList<Long> longs = new ArrayList<Long>();
        ResultSet rs = null;  // however you get this...
        while (rs.next()) {
            byte[] asBytes = rs.getBytes("myLongs");
            ByteArrayInputStream bin = new ByteArrayInputStream(asBytes);
            DataInputStream din = new DataInputStream(bin);
            for (int i = 0; i < asBytes.length/8; i++) {
                longs.add(din.readLong());
            }
            return longs;
        }
        return null;
    }
}
