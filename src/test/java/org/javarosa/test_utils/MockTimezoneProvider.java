package org.javarosa.test_utils;

import org.javarosa.core.model.utils.TimezoneProvider;

/**
 * Created by amstone326 on 1/8/18.
 */

public class MockTimezoneProvider extends TimezoneProvider {

    private int offsetMillis;

    public void setOffset(int offset) {
        this.offsetMillis = offset;
    }

    @Override
    public int getTimezoneOffsetMillis() {
        return offsetMillis;
    }
}
