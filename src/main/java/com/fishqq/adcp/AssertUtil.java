package com.fishqq.adcp;

import java.util.Objects;

public class AssertUtil {
    public static void assertEquals(Object expect, Object fact, String message) {
        if (!Objects.equals(expect, fact)) {
            throw new RuntimeException(String.format(
                    "%s: %s != %s",
                    message,
                    expect == null ? null : expect.toString(),
                    fact == null ? null : fact.toString()));
        }
    }

    public static void assertNotEquals(Object expect, Object fact, String message) {
        if (Objects.equals(expect, fact)) {
            throw new RuntimeException(String.format(
                    "%s: %s == %s",
                    message,
                    expect == null ? null : expect.toString(),
                    fact == null ? null : fact.toString()));
        }
    }

    public static void assertNull(Object fact, String message) {
        if (fact != null) {
            throw new RuntimeException(message + ": " + fact);
        }
    }
}
