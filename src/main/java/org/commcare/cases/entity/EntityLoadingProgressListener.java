package org.commcare.cases.entity;

/**
 * Listener to track progress of loading an entity list
 */
public interface EntityLoadingProgressListener {

    enum EntityLoadingProgressPhase {

        /**
         * Phase in which we build entity models from entity references, includes case detail calculations
         * normally except when we are lazy loading entities.
         * This is the only relevant phase for normal (non cache and index) case lists.
         */
        PHASE_PROCESSING(1),

        /**
         * Only relevant when entity cache is enabled, involves loading the entity cache into memory
         */
        PHASE_CACHING(2),


        /**
         * Phase in which we calculate any uncached entity fields. Can take much longer if things are not
         * already cached and similarly can be very quick when most things are not available in cache.
         */
        PHASE_UNCACHED_CALCULATION(3);

        private final int value;

        private EntityLoadingProgressPhase(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static EntityLoadingProgressPhase fromInt(int i) {
            for (EntityLoadingProgressPhase phase : EntityLoadingProgressPhase.values()) {
                if (phase.getValue() == i) {
                    return phase;
                }
            }
            throw new IllegalArgumentException("Unexpected value: " + i);
        }
    }

    /**
     * Method to implement to listen to the entity loading progress
     *
     * @param phase    The specific phase of entity loading process
     * @param progress progress corresponding to the current entity loading phase
     * @param total    max progress corresponding to the current entity loading phase
     */
    void publishEntityLoadingProgress(EntityLoadingProgressPhase phase, int progress, int total);
}
