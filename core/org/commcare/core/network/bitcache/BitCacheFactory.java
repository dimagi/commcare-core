package org.commcare.core.network.bitcache;

import java.io.File;

/**
 * @author ctsims
 */
public class BitCacheFactory {
    public static BitCache getCache(CacheDirSetup cacheDirSetup, long estimatedSize) {
        if (estimatedSize == -1 || estimatedSize > 1024 * 1024 * 4) {
            return new FileBitCache(cacheDirSetup);
        }
        return new MemoryBitCache();
    }

    public interface CacheDirSetup {
        File getCacheDir();
    }
}
