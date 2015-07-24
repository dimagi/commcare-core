package org.commcare.api.util;

import org.commcare.api.models.TFCase;
import org.commcare.api.persistence.TableBuilder;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.util.externalizable.Externalizable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * Created by wpride1 on 6/16/15.
 */
public class TableUtil {

    public static HashMap<String, Object> getContentValues(Externalizable e) {

        try {

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OutputStream out = bos;

            try {
                e.writeExternal(new DataOutputStream(out));
                out.close();
            } catch (IOException e1) {
                e1.printStackTrace();
                throw new RuntimeException("Failed to serialize externalizable for content values");
            }
            byte[] blob = bos.toByteArray();

            HashMap<String, Object> values = new HashMap<String, Object>();

            if (e instanceof IMetaData) {
                IMetaData m = (IMetaData) e;
                for (String key : m.getMetaDataFields()) {
                    Object o = m.getMetaData(key);
                    if (o == null) {
                        continue;
                    }
                    String value = o.toString();
                    values.put(TableBuilder.scrubName(key), value);
                }
            }

            values.put("commcare_sql_record", blob);

            return values;
        } catch(Exception de){
            de.printStackTrace();
            return null;
        }
    }

    public static String getCreateCaseTableString() {
            TableBuilder builder = new TableBuilder(TFCase.STORAGE_KEY);
            builder.addData(new TFCase());
            builder.setUnique(TFCase.INDEX_CASE_ID);
            return builder.getTableCreateString();
    }


}
