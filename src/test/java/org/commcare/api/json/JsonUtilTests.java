package org.commcare.api.json;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.test.FormParseInit;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by willpride on 3/31/16.
 */
public class JsonUtilTests {
    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testIndexFromString() {
        FormParseInit fpi = new FormParseInit("/xforms/repeat.xml");
        FormDef formDef = fpi.getFormDef();
        FormIndex formIndexOne = JsonActionUtils.indexFromString("4_0,1_0,0", formDef);
        assert formIndexOne.getDepth() == 3;
        assert formIndexOne.getInstanceIndex() == 0;
        assert formIndexOne.getElementMultiplicity() == -1;
        assert formIndexOne.getLocalIndex() == 4;

        FormIndex formIndexTwo = JsonActionUtils.indexFromString("4_1,1_0,0", formDef);
        assert formIndexTwo.getDepth() == 3;
        assert formIndexTwo.getInstanceIndex() == 1;
        assert formIndexTwo.getElementMultiplicity() == -1;
        assert formIndexTwo.getLocalIndex() == 4;

        FormIndex formIndexThree = JsonActionUtils.indexFromString("5", formDef);
        assert formIndexThree.getDepth() == 1;
        assert formIndexThree.getInstanceIndex() == -1;
        assert formIndexThree.getElementMultiplicity() == -1;
        assert formIndexThree.getLocalIndex() == 5;

        FormIndex formIndexFour = JsonActionUtils.indexFromString("7_1,0", formDef);
        assert formIndexFour.getDepth() == 2;
        assert formIndexFour.getInstanceIndex() == 1;
        assert formIndexFour.getElementMultiplicity() == -1;
        assert formIndexFour.getLocalIndex() == 7;
    }
}
