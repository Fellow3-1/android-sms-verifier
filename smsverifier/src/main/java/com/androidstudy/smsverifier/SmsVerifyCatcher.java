package com.androidstudy.smsverifier;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lifecycle-aware helper that manages runtime SMS permissions and registers an {@link SmsReceiver}
 * to automatically intercept incoming SMS verification messages.
 */
public class SmsVerifyCatcher {

    public static final int PERMISSION_REQUEST_CODE = 12;
    private static final Pattern DEFAULT_CODE_PATTERN = Pattern.compile("\\b\\d{4,8}\\b");

    private Activity activity;
    private Fragment fragment;
    private final OnSmsCatchListener<String> onSmsCatchListener;
    private SmsReceiver smsReceiver;
    private String phoneNumber;
    private String filter;
    private boolean isRegistered = false;

    public SmsVerifyCatcher(@NonNull Activity activity,
                            @NonNull OnSmsCatchListener<String> onSmsCatchListener) {
        this.activity = activity;
        this.onSmsCatchListener = onSmsCatchListener;
        this.smsReceiver = new SmsReceiver();
        this.smsReceiver.setCallback(this.onSmsCatchListener);
    }

    public SmsVerifyCatcher(@NonNull Activity activity,
                            @Nullable Fragment fragment,
                            @NonNull OnSmsCatchListener<String> onSmsCatchListener) {
        this(activity, onSmsCatchListener);
        this.fragment = fragment;
    }

    public SmsVerifyCatcher(@NonNull Fragment fragment,
                            @NonNull OnSmsCatchListener<String> onSmsCatchListener) {
        this(fragment.requireActivity(), fragment, onSmsCatchListener);
    }

    /**
     * Call from {@code Activity.onStart()} or {@code Fragment.onStart()}.
     * Requests runtime {@code RECEIVE_SMS} permission if needed and registers the SMS receiver.
     */
    public void onStart() {
        Activity hostActivity = resolveActivity();
        if (hostActivity == null) {
            return;
        }
        if (isSmsPermissionGranted(hostActivity, fragment)) {
            registerReceiver();
        }
    }

    private void registerReceiver() {
        Activity hostActivity = resolveActivity();
        if (isRegistered || hostActivity == null || hostActivity.isFinishing()) {
            return;
        }
        if (smsReceiver == null) {
            smsReceiver = new SmsReceiver();
        }
        smsReceiver.setCallback(onSmsCatchListener);
        smsReceiver.setPhoneNumberFilter(phoneNumber);
        smsReceiver.setFilter(filter);

        IntentFilter intentFilter = new IntentFilter(SmsReceiver.SMS_RECEIVED_ACTION);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hostActivity.registerReceiver(
                    smsReceiver,
                    intentFilter,
                    Manifest.permission.BROADCAST_SMS,
                    null,
                    Context.RECEIVER_EXPORTED
            );
        } else {
            hostActivity.registerReceiver(
                    smsReceiver,
                    intentFilter,
                    Manifest.permission.BROADCAST_SMS,
                    null
            );
        }
        isRegistered = true;
    }

    /**
     * Filter incoming SMS messages by sender phone number, prefix (e.g. "2547"), or sender ID.
     * Pass {@code null} or empty string to clear the filter.
     */
    public void setPhoneNumberFilter(@Nullable String phoneNumber) {
        this.phoneNumber = phoneNumber;
        if (smsReceiver != null) {
            smsReceiver.setPhoneNumberFilter(phoneNumber);
        }
    }

    /**
     * Filter incoming SMS messages by body regular expression.
     * Pass {@code null} or empty string to clear the filter.
     */
    public void setFilter(@Nullable String regexp) {
        this.filter = regexp;
        if (smsReceiver != null) {
            smsReceiver.setFilter(regexp);
        }
    }

    /**
     * Call from {@code Activity.onStop()} or {@code Fragment.onStop()} to unregister the receiver.
     */
    public void onStop() {
        Activity hostActivity = resolveActivity();
        if (isRegistered && hostActivity != null && smsReceiver != null) {
            try {
                hostActivity.unregisterReceiver(smsReceiver);
            } catch (IllegalArgumentException ignore) {
                // Receiver was already unregistered
            } finally {
                isRegistered = false;
            }
        }
    }

    /**
     * Returns {@code true} when the underlying {@link BroadcastReceiver} is actively registered.
     */
    public boolean isRegistered() {
        return isRegistered;
    }

    /**
     * Forward {@code onRequestPermissionsResult} from your Activity or Fragment so the SMS
     * receiver is automatically registered as soon as runtime permission is granted.
     */
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }
        if (grantResults == null || grantResults.length == 0) {
            // Permission prompt was cancelled or interrupted
            return;
        }

        boolean receiveSmsGranted = false;
        if (permissions != null && permissions.length == grantResults.length) {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.RECEIVE_SMS.equals(permissions[i])
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    receiveSmsGranted = true;
                    break;
                }
            }
        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            receiveSmsGranted = true;
        }

        if (receiveSmsGranted) {
            registerReceiver();
        }
    }

    /**
     * Checks whether {@link Manifest.permission#RECEIVE_SMS} is granted, requesting it at runtime
     * on Android 6.0 (API 23) and above if not yet granted.
     */
    public static boolean isSmsPermissionGranted(@NonNull Activity activity,
                                                 @Nullable Fragment fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECEIVE_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                String[] requiredPermissions = new String[]{Manifest.permission.RECEIVE_SMS};
                if (fragment == null) {
                    ActivityCompat.requestPermissions(activity, requiredPermissions, PERMISSION_REQUEST_CODE);
                } else {
                    fragment.requestPermissions(requiredPermissions, PERMISSION_REQUEST_CODE);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * @deprecated Renamed to {@link #isSmsPermissionGranted(Activity, Fragment)}.
     */
    @Deprecated
    public static boolean isStoragePermissionGranted(@NonNull Activity activity,
                                                     @Nullable Fragment fragment) {
        return isSmsPermissionGranted(activity, fragment);
    }

    /**
     * Extracts the first 4-to-8 digit numeric verification code from an SMS message.
     *
     * @param message incoming SMS message text
     * @return extracted code, or empty string {@code ""} if none found
     */
    @NonNull
    public static String parseCode(@Nullable String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        Matcher matcher = DEFAULT_CODE_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return "";
    }

    /**
     * Extracts the first numeric verification code with the exact specified number of digits.
     *
     * @param message    incoming SMS message text
     * @param codeLength expected digit count (e.g. 4 or 6)
     * @return extracted code, or empty string {@code ""} if none found
     */
    @NonNull
    public static String parseCode(@Nullable String message, int codeLength) {
        if (codeLength <= 0) {
            throw new IllegalArgumentException("codeLength must be positive, got: " + codeLength);
        }
        if (message == null || message.isEmpty()) {
            return "";
        }
        Pattern pattern = Pattern.compile("\\b\\d{" + codeLength + "}\\b");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return "";
    }

    @Nullable
    private Activity resolveActivity() {
        if (activity != null) {
            return activity;
        }
        if (fragment != null) {
            activity = fragment.getActivity();
        }
        return activity;
    }
}
