package com.androidstudy.smsverifier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SmsVerifyCatcher} code extraction and {@link SmsReceiver} filtering.
 */
public class ExampleUnitTest {

    @Test
    public void parseCode_extractsFourAndSixDigitCodes() {
        assertEquals("4829", SmsVerifyCatcher.parseCode("Your verification code is 4829."));
        assertEquals("482910", SmsVerifyCatcher.parseCode("Your Android SMS Verifier code is 482910. Valid for 5 minutes."));
        assertEquals("12345678", SmsVerifyCatcher.parseCode("Security code: 12345678"));
    }

    @Test
    public void parseCode_returnsFirstCodeWhenMultipleNumbersExist() {
        String sms = "Use OTP 5839 to sign in. Expires in 1800 seconds.";
        assertEquals("5839", SmsVerifyCatcher.parseCode(sms));
    }

    @Test
    public void parseCode_withExplicitLengthMatchesExactDigits() {
        String sms = "Ref 2026: your 6-digit OTP is 918273.";
        assertEquals("918273", SmsVerifyCatcher.parseCode(sms, 6));
        assertEquals("2026", SmsVerifyCatcher.parseCode(sms, 4));
    }

    @Test
    public void parseCode_handlesNullEmptyAndNoCode() {
        assertEquals("", SmsVerifyCatcher.parseCode(null));
        assertEquals("", SmsVerifyCatcher.parseCode(""));
        assertEquals("", SmsVerifyCatcher.parseCode("Hello! Call me in 5 minutes."));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseCode_rejectsNonPositiveLength() {
        SmsVerifyCatcher.parseCode("1234", 0);
    }

    @Test
    public void matchesPhoneNumber_supportsExactPrefixFormattedAndSenderIds() {
        assertTrue(SmsReceiver.matchesPhoneNumber("+254712345678", null));
        assertTrue(SmsReceiver.matchesPhoneNumber("+254712345678", ""));
        assertTrue(SmsReceiver.matchesPhoneNumber("777", "777"));
        assertTrue(SmsReceiver.matchesPhoneNumber("+254712345678", "2547"));
        assertTrue(SmsReceiver.matchesPhoneNumber("+254 712-345-678", "+254712345678"));
        assertTrue(SmsReceiver.matchesPhoneNumber("AFRICASTKNG", "africastkng"));

        assertFalse(SmsReceiver.matchesPhoneNumber("+15551234567", "2547"));
        assertFalse(SmsReceiver.matchesPhoneNumber(null, "777"));
        assertFalse(SmsReceiver.matchesPhoneNumber("", "777"));
    }

    @Test
    public void matchesMessageFilter_supportsMultilineAndInvalidRegexFallback() {
        String multilineSms = "<#> Your App code is 482910.\nDo not share this code.\nFA+9qCX9VSu";
        assertTrue(SmsReceiver.matchesMessageFilter(multilineSms, null));
        assertTrue(SmsReceiver.matchesMessageFilter(multilineSms, ""));
        assertTrue(SmsReceiver.matchesMessageFilter(multilineSms, ".*482910.*"));
        assertTrue(SmsReceiver.matchesMessageFilter(multilineSms, "\\b\\d{6}\\b"));
        assertFalse(SmsReceiver.matchesMessageFilter(multilineSms, "999999"));
        assertFalse(SmsReceiver.matchesMessageFilter(null, "\\d+"));

        // Unclosed bracket is invalid regex; falls back to literal match without throwing
        assertTrue(SmsReceiver.matchesMessageFilter("[MyApp] Code: 1234", "[MyApp"));
        assertFalse(SmsReceiver.matchesMessageFilter("Plain SMS: 1234", "[MyApp"));
    }

    @Test
    public void smsReceiver_ignoresNullIntentSafely() {
        SmsReceiver receiver = new SmsReceiver();
        receiver.onReceive(null, null);
    }
}
