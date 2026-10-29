package org.commcare.util.screen;
public class ScreenContext {

    private boolean respectRelevancy;

    public ScreenContext(boolean respectRelevancy) {
        this.respectRelevancy = respectRelevancy;
    }

    public boolean isRespectRelevancy() {
        return respectRelevancy;
    }
}
