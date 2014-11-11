package org.commcare.suite.model.graph;

import java.util.Date;

public class TimePointData extends XYPointData {
    private Date mTime;

    public TimePointData(Date time, Double y) {
        super(null, y.toString());
        mTime = time;
    }
    
    public Date getTime() {
        return mTime;
    }
    
}
