package org.commcare.cases.entity;


import static org.commcare.cases.entity.EntityLoadingProgressListener.EntityLoadingProgressPhase.PHASE_UNCACHED_CALCULATION;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.CacheHost;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nullable;
/**
 * @author ctsims
 */
public class AsyncNodeEntityFactory extends NodeEntityFactory {
    private final OrderedHashtable<String, XPathExpression> mVariableDeclarations;

    private final Hashtable<String, AsyncEntity> mEntitySet = new Hashtable<>();
    @Nullable
    private final EntityStorageCache mEntityCache;

    private CacheHost mCacheHost = null;
    private Boolean mTemplateIsCachable = null;
    private static final Object mAsyncLock = new Object();
    private Thread mAsyncPrimingThread;

    // Don't show entity list until we primeCache and caches all fields
    private final boolean isBlockingAsyncMode;

    /**
     * Whether we are loading entity in a background process.
     * Used to accelerate processing in foreground by skipping lazy properties
     */
    private boolean inBackground;

    public AsyncNodeEntityFactory(Detail d, EvaluationContext ec,
            @Nullable EntityStorageCache entityStorageCache, boolean inBackground) {
        super(d, ec);
        mVariableDeclarations = detail.getVariableDeclarations();
        mEntityCache = entityStorageCache;
        isBlockingAsyncMode = detail.hasSortField();
        this.inBackground = inBackground;
    }

    @Override
    public Entity<TreeReference> getEntity(TreeReference data) {
        EvaluationContext nodeContext = new EvaluationContext(ec, data);

        mCacheHost = nodeContext.getCacheHost(data);

        String mCacheIndex = null;
        if (mTemplateIsCachable == null) {
            mTemplateIsCachable = mCacheHost != null && mCacheHost.isReferencePatternCachable(data);
        }
        if (mTemplateIsCachable && mCacheHost != null) {
            mCacheIndex = mCacheHost.getCacheIndex(data);
        }

        String entityKey = loadCalloutDataMapKey(nodeContext);
        AsyncEntity entity =
                new AsyncEntity(detail, nodeContext, data, mVariableDeclarations,
                        mEntityCache, mCacheIndex, entityKey);

        if (mCacheIndex != null) {
            mEntitySet.put(mCacheIndex, entity);
        }
        return entity;
    }

    @Override
    protected void setEvaluationContextDefaultQuerySet(EvaluationContext ec,
                                                       List<TreeReference> result) {

        //Don't do anything for asynchronous lists. In theory the query set could help expand the
        //first cache more quickly, but otherwise it's just keeping around tons of cases in memory
        //that don't even need to be loaded.
    }


    /**
     * Bulk loads search field cache from db.
     * Note that the cache is lazily built upon first case list search.
     */
    protected void primeCache() {
        if (isCancelled) return;
        if (mEntityCache == null || mTemplateIsCachable == null || !mTemplateIsCachable || mCacheHost == null) {
            return;
        }

        String[][] cachePrimeKeys = mCacheHost.getCachePrimeGuess();
        if (cachePrimeKeys == null) {
            return;
        }
        updateProgress(EntityLoadingProgressListener.EntityLoadingProgressPhase.PHASE_CACHING, 0, 100);
        mEntityCache.primeCache(mEntitySet,cachePrimeKeys, detail);
        updateProgress(EntityLoadingProgressListener.EntityLoadingProgressPhase.PHASE_CACHING, 100, 100);
    }

    private void updateProgress(EntityLoadingProgressListener.EntityLoadingProgressPhase phase, int progress,
            int total) {
        if (progressListener != null) {
            progressListener.publishEntityLoadingProgress(phase, progress, total);
        }
    }

    @Override
    protected void prepareEntitiesInternal(List<Entity<TreeReference>> entities) {
        // Legacy cache and index code, only here to maintain backward compatibility
        // if blocking mode load cache on the same thread and set any data thats not cached
        if (isBlockingAsyncMode) {
            primeCache();
            setUnCachedDataOld(entities);
        } else {
            // otherwise we want to show the entity list asap and hence want to offload the loading cache part to a separate
            // thread while caching any uncached data later on UI thread during Adapter's getView
            synchronized (mAsyncLock) {
                if (mAsyncPrimingThread == null) {
                    mAsyncPrimingThread = new Thread(this::primeCache);
                    mAsyncPrimingThread.start();
                }
            }
        }
    }

    @Override
    public void cacheEntities(List<Entity<TreeReference>> entities) {
        if (detail.isCacheEnabled()) {
            primeCache();
            setUnCachedData(entities);
        } else {
            primeCache();
            setUnCachedDataOld(entities);
        }
    }

    protected void setUnCachedData(List<Entity<TreeReference>> entities) {
        boolean foregroundWithLazyLoading = !inBackground && detail.isLazyLoading();
        boolean foregroundWithoutLazyLoading = !inBackground && !detail.isLazyLoading();
        for (int i = 0; i < entities.size(); i++) {
            if (isCancelled) return;
            AsyncEntity e = (AsyncEntity)entities.get(i);
            for (int col = 0; col < e.getNumFields(); ++col) {
                DetailField field = detail.getFields()[col];
                /**
                 * 1. If we are in foreground with lazy loading turned on, the priority is to show
                 * the user screen asap. Therefore, we need to skip calculating lazy fields.
                 * 2. If we are in foreground with lazy loading turned off, we want to calculate all fields here.
                 * 3. If we are in background with lazy loading turned on or off, we want to calculate all fields
                 * backed by cache in order to keep them ready for when user loads the list.
                 */
                if (foregroundWithoutLazyLoading || (foregroundWithLazyLoading && !field.isLazyLoading()) || (
                        inBackground && field.isCacheEnabled())) {
                    e.getField(col);
                    if (field.getSort() != null) {
                        e.getSortField(col);
                    }
                }
            }
            if (i % 100 == 0) {
                updateProgress(PHASE_UNCACHED_CALCULATION, i, entities.size());
            }
        }
        updateProgress(PHASE_UNCACHED_CALCULATION, entities.size(), entities.size());
    }

    // Old cache and index pathway where we only cache sort fields
    @Deprecated
    protected void setUnCachedDataOld(List<Entity<TreeReference>> entities) {
        for (int i = 0; i < entities.size(); i++) {
            if (isCancelled) return;
            AsyncEntity e = (AsyncEntity)entities.get(i);
            for (int col = 0; col < e.getNumFields(); ++col) {
                    e.getSortField(col);
            }
            updateProgress(PHASE_UNCACHED_CALCULATION, i, entities.size());
        }
    }

    @Override
    protected boolean isEntitySetReadyInternal() {
        synchronized (mAsyncLock) {
            return mAsyncPrimingThread == null || !mAsyncPrimingThread.isAlive();
        }
    }

    public boolean isBlockingAsyncMode() {
        return isBlockingAsyncMode;
    }
}
