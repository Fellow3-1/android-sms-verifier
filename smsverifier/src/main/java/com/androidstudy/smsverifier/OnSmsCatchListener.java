package com.androidstudy.smsverifier;

/**
 * Callback listener invoked when an incoming SMS matching configured filters is intercepted.
 *
 * @param <T> payload type (typically {@link String})
 */
public interface OnSmsCatchListener<T> {
    void onSmsCatch(T message);
}
