package com.ute.mobi.utilities;

/**
 * Created by jonathanliono on 8/01/15.
 */
public interface HttpAsyncTaskCallbacks {
    void onSuccess(String result);
    void onErrorUnauthorized();
    void onErrorGeneralRequest(int statusCode);
    void onNoNetworkAvailable();
    void onExceptionThrown(Exception e);
}
