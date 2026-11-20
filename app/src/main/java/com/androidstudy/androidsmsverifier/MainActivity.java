package com.androidstudy.androidsmsverifier;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.androidstudy.smsverifier.OnSmsCatchListener;
import com.androidstudy.smsverifier.SmsVerifyCatcher;

public class MainActivity extends AppCompatActivity {

    private SmsVerifyCatcher smsVerifyCatcher;
    private EditText etCode;
    private TextView tvStatus;
    private TextView tvLastSms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCode = findViewById(R.id.et_code);
        tvStatus = findViewById(R.id.tv_status);
        tvLastSms = findViewById(R.id.tv_last_sms);
        final EditText etSenderFilter = findViewById(R.id.et_sender_filter);
        final Button btnVerify = findViewById(R.id.btn_verify);
        final Button btnSimulateSms = findViewById(R.id.btn_simulate_sms);

        // Initialize SmsVerifyCatcher to intercept incoming SMS messages
        smsVerifyCatcher = new SmsVerifyCatcher(this, new OnSmsCatchListener<String>() {
            @Override
            public void onSmsCatch(String message) {
                handleIncomingSms(message);
            }
        });

        // Allow optional live filtering by sender phone number / prefix (e.g. "2547")
        etSenderFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                smsVerifyCatcher.setPhoneNumberFilter(s != null ? s.toString().trim() : null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Verify button validates the entered or auto-filled verification code
        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyEnteredCode();
            }
        });

        // Demo helper to preview SMS parsing and auto-fill on devices without cellular service
        btnSimulateSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleIncomingSms(getString(R.string.sample_simulated_sms));
            }
        });
    }

    private void handleIncomingSms(String message) {
        String code = parseCode(message);
        if (!code.isEmpty()) {
            etCode.setError(null);
            etCode.setText(code);
            etCode.setSelection(code.length());
            updateStatus(getString(R.string.status_code_caught), false);
        }
        if (message != null && !message.trim().isEmpty()) {
            tvLastSms.setText("SMS: \"" + message.trim() + "\"");
            tvLastSms.setVisibility(View.VISIBLE);
        }
    }

    private void verifyEnteredCode() {
        String entered = etCode.getText() != null ? etCode.getText().toString().trim() : "";
        if (!entered.matches("\\d{4,8}")) {
            etCode.setError(getString(R.string.error_invalid_code));
            etCode.requestFocus();
            return;
        }
        etCode.setError(null);
        updateStatus(getString(R.string.status_verified), true);
        Toast.makeText(this, R.string.status_verified, Toast.LENGTH_SHORT).show();
    }

    private void updateStatus(String statusMessage, boolean verified) {
        tvStatus.setText(statusMessage);
        if (verified) {
            tvStatus.setBackgroundResource(R.drawable.bg_status_verified);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.colorSuccess));
        } else {
            tvStatus.setBackgroundResource(R.drawable.bg_status_listening);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }
    }

    /**
     * Parse a 4-to-8 digit verification code from an SMS body.
     *
     * @param message incoming SMS message
     * @return extracted numeric verification code, or empty string if none found
     */
    static String parseCode(String message) {
        return SmsVerifyCatcher.parseCode(message);
    }

    @Override
    protected void onStart() {
        super.onStart();
        smsVerifyCatcher.onStart();
        refreshListeningStatus();
    }

    @Override
    protected void onStop() {
        super.onStop();
        smsVerifyCatcher.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        smsVerifyCatcher.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshListeningStatus();
    }

    private void refreshListeningStatus() {
        String currentCode = etCode.getText() != null ? etCode.getText().toString().trim() : "";
        if (!currentCode.isEmpty()) {
            return;
        }
        if (smsVerifyCatcher.isRegistered()) {
            updateStatus(getString(R.string.status_listening), false);
        } else {
            updateStatus(getString(R.string.status_permission_needed), false);
        }
    }
}
