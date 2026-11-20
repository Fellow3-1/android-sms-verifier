package com.androidstudy.androidsmsverifier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Local unit tests for sample app OTP verification code parsing.
 */
public class ExampleUnitTest {

    @Test
    public void parseCode_extractsOtpFromSampleMessages() {
        assertEquals(
                "482910",
                MainActivity.parseCode("Your Android SMS Verifier code is 482910. Valid for 5 minutes.")
        );
        assertEquals(
                "7391",
                MainActivity.parseCode("Verification code: 7391")
        );
        assertEquals(
                "",
                MainActivity.parseCode("Welcome to Android SMS Verifier!")
        );
    }
}
