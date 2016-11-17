package org.javarosa.core.model.data;

/**
 * Created by Saumya on 7/25/2016.
 */
public class InvalidDateData extends InvalidData {

    private final String dayText;
    private final String monthText;
    private final String yearText;

    public InvalidDateData(String error, IAnswerData returnValue, String day, String month, String year){
        super(error, returnValue);
        dayText = day;
        monthText = month;
        yearText = year;
    }

    public String getDayText(){
        return dayText;
    }

    public String getMonthText(){
        return monthText;
    }

    public String getYearText(){
        return yearText;
    }
}
