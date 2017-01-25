package org.commcare.cases.util;

import org.commcare.cases.model.Case;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Vector;

/**
 * Created by ctsims on 1/25/2017.
 */

public class CaseModelFetchCue implements QueryCue {
    boolean active = false;
    boolean isPrimed = false;

    static final int chunk_size = 100;

    LinkedHashSet<Integer> ids;

    HashMap<Integer, Case> caseMap;

    public CaseModelFetchCue(Vector<Integer> ids) {
        this.ids = new LinkedHashSet<>(ids);
    }

    @Override
    public void cleanupCue() {
        isPrimed = false;
        if(caseMap != null ) {
            caseMap.clear();
        }
        if(ids != null) {
            ids.clear();
        }
    }

    @Override
    public void activate() {
        active = true;
    }

    public boolean isCueing(int recordId) {
        if(active && ids.contains(recordId)) {
            return true;
        } else {
            return false;
        }
    }

    public LinkedHashSet<Integer> getCuedCases() {
        return ids;
    }

    public boolean isPrimed(int recordId) {
        return isPrimed;
    }

    public void setPrimed() {
        isPrimed = true;
    }

    public Case getCase(int recordId) {
        return caseMap.get(recordId);
    }

    public HashMap<Integer,Case> getCaseMap() {
        if(caseMap == null ){
            caseMap = new HashMap<>();
        }
        return caseMap;
    }
}
