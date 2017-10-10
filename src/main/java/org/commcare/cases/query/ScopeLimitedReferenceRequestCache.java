package org.commcare.cases.query;

import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This cache keeps track of the *full* set of TreeReferences which will
 * be dereferenced within the scope of a "limited" query. This can be used
 * by DataInstances to identify that they do not need to load full data models
 * within the scope of a given query, but only those needed to meet the limited
 * query scope.
 *
 * This cache should only get created in a query context which is guaranteed to be
 * transient.
 *
 * Created by ctsims on 9/18/2017.
 */

public class ScopeLimitedReferenceRequestCache implements QueryCache {

    //TODO: Do we get anything from sealing the scope other than vague confidence?

    private final String DEFAULT_INSTANCE_KEY = "/";

    // Maps instance names to the set of tree references that are "in scope" (i.e. that we must be
    // able to evaluate) for that instance in the current context
    private HashMap<String, Set<TreeReference>> map = new HashMap<>();

    /**
     * Replaces the tree element cache used to hold the final partial tree elements, since
     * the in-instance cache isn't safe to store those partial elements
     */
    private HashMap<String, HashMap<Integer, TreeElement>> treeElementCache
            = new HashMap<>();

    /**
     * A list of instance names for instances which have flagged that they cannot utilize
     * the limited scope.
     */
    private Set<String> excludedInstances = new HashSet<>();

    private HashMap<String, String[]> instanceScopeLimitCache = new HashMap<>();

    public ScopeLimitedReferenceRequestCache() {

    }

    public void addTreeReferencesToLimitedScope(Set<TreeReference> references) {
        for (TreeReference reference : references) {
            String instanceName = reference.getInstanceName();
            if (instanceName == null) {
                instanceName = DEFAULT_INSTANCE_KEY;
            }

            Set<TreeReference> existingRefs;
            if (map.containsKey(instanceName)) {
                existingRefs = map.get(instanceName);
            } else {
                existingRefs = new HashSet<>();
            }

            existingRefs.add(reference);
            map.put(instanceName, existingRefs);
        }
    }

    /**
     * @returns true if an instance has a limited scope to report, and has not explicitly informed
     * the cache that it should be excluded from future requests.
     */
    public boolean isInstancePotentiallyScopeLimited(String instanceName) {
        return map.containsKey(instanceName) && !excludedInstances.contains(instanceName);
    }

    /**
     * Get all of the in-scope tree references for the provided instance
     */
    public Set<TreeReference> getInScopeReferences(String instanceName) {
        return map.get(instanceName);
    }

    /**
     * Signal that the scope limit is  unhelpful for the provided instance. Will prevent
     * that instance from being included in future requests to isInstancePotentiallyScopeLimited
     */
    public void setScopeLimitUnhelpful(String instanceName) {
        excludedInstances.add(instanceName);
    }


    public TreeElement getCachedElementIfExists(String instanceName, int recordId) {
        if (!treeElementCache.containsKey(instanceName) || !treeElementCache.get(instanceName).containsKey(recordId)) {
            return null;
        }
        return treeElementCache.get(instanceName).get(recordId);
    }

    public void cacheElement(String instanceName, int recordId, TreeElement element) {
        treeElementCache.get(instanceName).put(recordId, element);
    }

    /**
     * Provide a cue from within an instance that it has determined a payload that it can use to
     * process requests from limited scope.
     *
     * This signal is a guarantee that the instance will be able to dereference the
     * tree references it is responsible for in the limited scope using a partial load rather
     * than a full model load.
     */
    public void setInternalScopeLimit(String instanceName, String[] columnNameCacheLoads) {
        this.instanceScopeLimitCache.put(instanceName, columnNameCacheLoads);
        treeElementCache.put(instanceName, new HashMap());
    }

    /**
     * If the cache has already been provided with a payload for this limited scope request,
     * return it. Otherwises returns null.
     */
    public String[] getInternalScopedLimit(String instanceName) {
        return instanceScopeLimitCache.get(instanceName);
    }
}
