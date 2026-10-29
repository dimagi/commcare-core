package org.commcare.cases.entity;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xpath.expr.XPathExpression;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import static org.junit.Assert.assertEquals;

/**
 * Regression test: sort field expressions must not be evaluated during the cache
 * priming loop when no EntityStorageCache is present (formplayer always passes null).
 *
 * Before the fix, setUnCachedDataOld iterated every entity × every column and called
 * getSortField unconditionally, evaluating expensive XPath even though there was nowhere to
 * store the result.
 */
public class AsyncNodeEntityFactoryTest {

    @Test
    public void prepareEntities_doesNotCallGetSortField_whenCacheIsNull() {
        Detail detail = buildDetailWithSortField();

        AsyncNodeEntityFactory factory = new AsyncNodeEntityFactory(
                detail, new EvaluationContext(null), null, false);

        SpyAsyncEntity spy1 = new SpyAsyncEntity(detail);
        SpyAsyncEntity spy2 = new SpyAsyncEntity(detail);
        List<Entity<TreeReference>> entities = new ArrayList<>();
        entities.add(spy1);
        entities.add(spy2);

        factory.prepareEntities(entities);

        assertEquals("getSortField must not be called when cache is null", 0, spy1.getSortFieldCallCount);
        assertEquals("getSortField must not be called when cache is null", 0, spy2.getSortFieldCallCount);
    }

    private static Detail buildDetailWithSortField() {
        DetailField.Builder fieldBuilder = new DetailField.Builder();
        fieldBuilder.setSortOrder(1);
        fieldBuilder.setSort(Text.PlainText("sort_value"));

        Vector<DetailField> fields = new Vector<>();
        fields.add(fieldBuilder.build());

        return new Detail("test_detail", null, null, null,
                new Vector<>(), fields, new OrderedHashtable<>(),
                null, null, null, null, null, null, null, null, null, null,
                false, false, null);
    }

    private static class SpyAsyncEntity extends AsyncEntity {
        int getSortFieldCallCount = 0;

        SpyAsyncEntity(Detail detail) {
            super(detail, new EvaluationContext(null), new TreeReference(),
                    new Hashtable<String, XPathExpression>(), null, null, null);
        }

        @Override
        public String getSortField(int i) {
            getSortFieldCallCount++;
            return super.getSortField(i);
        }
    }

}
