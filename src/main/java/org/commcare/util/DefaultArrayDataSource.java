package org.commcare.util;

/**
 * Created by ctsims on 7/19/2017.
 */

public class DefaultArrayDataSource implements ArrayDataSource {

    private String[] ethiopian = new String[]{
            "Mäskäräm",
            "T’ïk’ïmt",
            "Hïdar",
            "Tahsas",
            "T’ïr",
            "Yäkatit",
            "Mägabit",
            "Miyaziya",
            "Gïnbot",
            "Säne",
            "Hämle",
            "Nähäse",
            "P’agume"};

    private String[] nepali = new String[]{
            "Baishakh",
            "Jestha",
            "Ashadh",
            "Shrawan",
            "Bhadra",
            "Ashwin",
            "Kartik",
            "Mangsir",
            "Poush",
            "Magh",
            "Falgun",
            "Chaitra"};

    @Override
    public String[] getArray(String key) {
        if("ethiopian_months".equals(key)) {
            return ethiopian;
        } else if("nepali_months".equals(key)) {
            return nepali;
        }
        throw new RuntimeException("No supported fallback month names for calendar: " + key);
    }
}
