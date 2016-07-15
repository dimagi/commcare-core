package org.javarosa.xform.util;

/**
 * Created by willpride on 7/15/16.
 */
public class UniversalDate {

    public static final long MILLIS_IN_DAY = 1000 * 60 * 60 * 24;

    public final int year;
    public final int month;
    public final int day;
    public final long millisFromJavaEpoch;

    public UniversalDate(int year, int month, int day, long millisFromJavaEpoch) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.millisFromJavaEpoch = millisFromJavaEpoch;
    }

}