package org.commcare.cases.query.queryset;

import org.commcare.cases.query.QueryContext;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.List;
import java.util.Set;

/**
 * A Query Set Lookup defines a way to match an (input) TreeReference to a set of ID's (output)
 * which refer to multiplicities in the current model.
 *
 * The key aspects of query set lookups are that they can occur in bulk (IE: Get all cases matching
 * a pattern) and that they can be transformed (IE: From a set of cases, you can get a query set
 * lookup which gives you all of those cases's parent cases)
 *
 * For Example: An implementing class may define that a specific reference looks up a case
 * in the database, and that due to a queryset that is currently cached, instead of evaluating
 * that reference raw, the lookup can be performed based on the current cache
 *
 * Important Note: The Query Set Lookup itself should _never_ persist or store any of the data
 * that it returns from the query lookups. State should be confined to the query context provided
 * to be managed by its lifecycle.
 *
 * Created by ctsims on 2/6/2017.
 */

public interface QuerySetLookup {

    /**
     * Checks the current evaluation context to see if if this lookup can establish a reference
     * which it knows how to perform with a query set, and returns it if so.
     *
     * The result of this expression will be a specific TreeReference element
     * IE: isntance('casedb')/casedb/case[15]
     *
     * which has a specific semantic meaning to this lookup and can be optimized
     *
     * @return null if this lookup can not identify elements in hte provided context. Otherwise
     * a "key" tree reference to be used by the other methods in this class
     */
    TreeReference getLookupIdKey(EvaluationContext evaluationContext);

    /**
     * Given a "Key" from getLookupIdKey, establishes whether the current query context actually
     * has the data it needs to look up the provided key.
     *
     * Note: LookupIdKey can be null, so you can use the result of getLookupIdKey directly,
     * regardless of whether it returned null
     *
     * @return True if this query set lookup can identify a query set result for the provided
     * key given the query context
     */
    boolean isValid(TreeReference lookupIdKey, QueryContext context);

    /**
     * @return A semantic value identifying the "model type" that is being returned by this
     * query set lookup. Other lookups or optimizations may only be valid depending on this
     * model query type.
     *
     * Example: "CASE" <- Meaning that the result of this query set is an ID into the case data
     * model.
     */
    String getQueryModelId();

    /**
     * @return a key that identifies the Model Query Set this lookup will use, and should used to
     * identify/resolve the query set in the current context.
     *
     * EXAMPLE: "case_currrent" <- the set of cases which are returned by current()
     */
    String getCurrentQuerySetId();

    /**
     * Takes in a lookup ID key and provides the query set results for that key in particular.
     *
     * Example:
     *
     * CaseParentIndexQuerySetLookup looks up the case model id of cases which have a 'parent'
     * index to other cases.
     *
     * lookupIdKey is a casedb reference
     *
     * This method returns a list of case model id's that have a parent index to the case identified
     * by the provided lookupIdKey
     *
     */
    List<Integer> performSetLookup(TreeReference lookupIdKey, QueryContext queryContext);

    /**
     *
     * For the provided query context, gets all model ID's which could be matched and returned
     * by a set lookup. Used to provide the capability to transform lookups in bulk rather than
     * one-at-a-time.
     *
     * Example:
     *
     * CaseParentIndexQuerySetLookup looks up the case model id of cases which have a 'parent'
     * index to other cases.
     *
     * This method returns a list of all model ID's to cases which would be the result of a
     * performSetLookup operation on lookupIdKeys in the current context.
     */
    Set<Integer> getLookupSetBody(QueryContext queryContext);
}
