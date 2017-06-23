package org.commcare.modern.util;

/**
 * A catch-all for centralizing hooks and state for tuning on performance optimizations which may
 * need to shift constants or boundaries based on the current platform
 *
 * Created by ctsims on 6/21/2017.
 */

public class PerformanceTuningUtil {
    private static int MAX_PREFETCH_CASE_BLOCK = -1;

    private static final long MB_64 = 64 * 1024 * 2014;
    private static final long MB_256 = 256 * 1024 * 2014;
    private static final long MB_1024 = 1024 * 1024 * 2014;

    /**
     * Update the constant size of cases eligible for batch "pre-fetch" in db ops. This
     * is roughly the number of cases which can be expected to reliably fit into memory without
     * lowering the available heap sufficiently to introduce stability concerns.
     */
    public static void updateMaxPrefetchCaseBlock(int newMaxSize) {
        MAX_PREFETCH_CASE_BLOCK = newMaxSize;
    }

    /**
     * @return A heuristic for the largest safe value for block prefetch based on the
     * current device runtime.
     */
    public static int guessLargestSupportedBulkCaseFetchSizeFromHeap() {
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();

        return guessLargestSupportedBulkCaseFetchSizeFromHeap(maxMemory);
    }

    /**
     * @return A heuristic for the largest safe value for block prefetch based on a provided
     * amount of memory which should be available for optimizations.
     */
    public static int guessLargestSupportedBulkCaseFetchSizeFromHeap(long availableMemoryInBytes) {
        //NOTE: These are just tuned from experience and testing on mobile devices around values
        //which prevent them from running out of memory. It would be well worth it in the
        //future to take a more comprehensive approach.

        if (availableMemoryInBytes == 0 || availableMemoryInBytes == -1) {
            //This was the existing tuned default, so don't change it until we have a better guess.
            return 7500;
        } else {
            if (availableMemoryInBytes <= MB_64) {
                return 2500;
            } else if (availableMemoryInBytes <= MB_256) {
                return 7500;
            } else if (availableMemoryInBytes <= MB_1024) {
                return 15000;
            } else {
                return 50000;
            }
        }
    }

    /*
     * @return The maximum number of cases which will be included in a "Pre-Fetch Batch".
     */
    public static int getMaxPrefetchCaseBlock() {
        if (MAX_PREFETCH_CASE_BLOCK == -1) {
            updateMaxPrefetchCaseBlock(guessLargestSupportedBulkCaseFetchSizeFromHeap());
        }
        return MAX_PREFETCH_CASE_BLOCK;
    }
}
