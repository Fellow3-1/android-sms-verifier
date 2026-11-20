package com.androidstudy.smsverifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Internal {@link BroadcastReceiver} that intercepts incoming {@code SMS_RECEIVED} broadcasts,
 * reassembles multipart SMS PDUs, applies optional sender and regex filters, and notifies
 * {@link OnSmsCatchListener}.
 */
class SmsReceiver extends BroadcastReceiver {

    static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
    private static final String TAG = "SmsReceiver";

    private OnSmsCatchListener<String> callback;
    private String phoneNumberFilter;
    private String filter;

    /**
     * Set result callback.
     *
     * @param callback OnSmsCatchListener
     */
    public void setCallback(@Nullable OnSmsCatchListener<String> callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        try {
            final Bundle bundle = intent.getExtras();
            if (bundle == null) {
                return;
            }

            final Object[] pdusObj = (Object[]) bundle.get("pdus");
            if (pdusObj == null || pdusObj.length == 0) {
                return;
            }

            // Reassemble multipart SMS messages per originating address in arrival order
            Map<String, StringBuilder> messagesBySender = new LinkedHashMap<>();
            for (Object pduObj : pdusObj) {
                if (!(pduObj instanceof byte[])) {
                    continue;
                }
                SmsMessage currentMessage = getIncomingMessage((byte[]) pduObj, bundle);
                if (currentMessage == null) {
                    continue;
                }

                String sender = currentMessage.getDisplayOriginatingAddress();
                if (sender == null) {
                    sender = "";
                }
                String bodyPart = currentMessage.getDisplayMessageBody();
                if (bodyPart == null) {
                    continue;
                }

                StringBuilder builder = messagesBySender.get(sender);
                if (builder == null) {
                    builder = new StringBuilder();
                    messagesBySender.put(sender, builder);
                }
                builder.append(bodyPart);
            }

            for (Map.Entry<String, StringBuilder> entry : messagesBySender.entrySet()) {
                String sender = entry.getKey();
                String fullMessage = entry.getValue().toString();

                if (!matchesPhoneNumber(sender, phoneNumberFilter)) {
                    continue;
                }
                if (!matchesMessageFilter(fullMessage, filter)) {
                    continue;
                }
                if (callback != null) {
                    callback.onSmsCatch(fullMessage);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while processing incoming SMS", e);
        }
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private SmsMessage getIncomingMessage(byte[] pdu, Bundle bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String format = bundle.getString("format");
            return SmsMessage.createFromPdu(pdu, format);
        } else {
            return SmsMessage.createFromPdu(pdu);
        }
    }

    /**
     * Checks whether the incoming sender address matches the configured phone number / sender filter.
     * Supports exact numbers, country-code prefixes (e.g. "2547" matching "+254712345678"),
     * shortcodes (e.g. "777"), and alphanumeric sender IDs.
     */
    static boolean matchesPhoneNumber(@Nullable String sender, @Nullable String phoneNumberFilter) {
        if (phoneNumberFilter == null || phoneNumberFilter.trim().isEmpty()) {
            return true;
        }
        if (sender == null || sender.trim().isEmpty()) {
            return false;
        }
        String normalizedSender = normalizePhoneNumber(sender).toLowerCase(Locale.ROOT);
        String normalizedFilter = normalizePhoneNumber(phoneNumberFilter.trim()).toLowerCase(Locale.ROOT);
        return normalizedSender.contains(normalizedFilter);
    }

    /**
     * Checks whether the incoming SMS body matches the configured regular expression filter.
     * Uses {@link Pattern#DOTALL} so multiline SMS messages are matched reliably.
     */
    static boolean matchesMessageFilter(@Nullable String message, @Nullable String regularExpression) {
        if (regularExpression == null || regularExpression.isEmpty()) {
            return true;
        }
        if (message == null) {
            return false;
        }
        try {
            return Pattern.compile(regularExpression, Pattern.DOTALL).matcher(message).find();
        } catch (PatternSyntaxException e) {
            Log.w(TAG, "Invalid regex filter supplied, falling back to literal match: " + regularExpression, e);
            return message.contains(regularExpression);
        }
    }

    @NonNull
    static String normalizePhoneNumber(@NonNull String value) {
        return value.replaceAll("[\\s\\-()]", "");
    }

    /**
     * Set phone number or sender ID filter.
     *
     * @param phoneNumberFilter phone number, prefix, or sender ID
     */
    public void setPhoneNumberFilter(@Nullable String phoneNumberFilter) {
        this.phoneNumberFilter = phoneNumberFilter;
    }

    /**
     * Set message filter with regular expression.
     *
     * @param regularExpression regular expression
     */
    public void setFilter(@Nullable String regularExpression) {
        this.filter = regularExpression;
    }
}
