package org.commcare.services;

/**
 * A SignalLevelProvider is a platform dependent tool which
 * is capable of identifying the current signal level on the
 * device being used.
 *
 * It doesn't necessarily provide detailed or accurate signal
 * levels, but can determine at the very least whether a packet data
 * connection is available.
 *
 * @author ctsims
 *
 */
public interface SignalLevelProvider {
    public boolean isDataPossible();
}
