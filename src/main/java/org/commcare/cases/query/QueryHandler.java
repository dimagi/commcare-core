package org.commcare.cases.query;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

/**
 * A tunable host for areas in storage which need potentially complex caching semantics
 * which rely on some structural knowledge. In many implementations will be quite simple
 *
 * Created by ctsims on 1/25/2017.
 */

public interface QueryHandler<T> {

    int getExpectedRuntime();

    T profileHandledQuerySet(Vector<PredicateProfile> profiles);

    List<Integer> loadProfileMatches(T querySet, QueryContext queryContext);

    void updateProfiles(T querySet, Vector<PredicateProfile> profiles);

    Collection<PredicateProfile> collectPredicateProfiles(
            Vector<XPathExpression> predicates, QueryContext context,
            EvaluationContext evaluationContext);
}
