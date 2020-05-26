package org.commcare.core.network;

import okhttp3.OkHttpClient;

public interface HttpBuilderConfig {
    OkHttpClient.Builder performCustomConfig(OkHttpClient.Builder client);
}
