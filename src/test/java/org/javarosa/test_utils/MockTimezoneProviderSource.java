package org.javarosa.test_utils;

import org.javarosa.core.model.utils.TimezoneProvider;
import org.javarosa.core.model.utils.TimezoneProviderSource;

/**
 * Created by amstone326 on 1/8/18.
 */

public class MockTimezoneProviderSource extends TimezoneProviderSource {

    private int offsetMillis;

    public void setOffset(int offset) {
        this.offsetMillis = offset;
    }

    @Override
    protected TimezoneProvider getProvider() {
        return new TimezoneProvider() {

            @Override
            public int getTimezoneOffsetMillis() {
                return offsetMillis;
            }

        };
    }
}
