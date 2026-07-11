package com.proops2026.userservice.util;

// MIN-40 SonarCloud new-code gate verification — DELETE after test.
// Deliberate Sonar Bug (java:S1764) + 0% coverage on new code. No secret.
public final class SonarCanary {

    private SonarCanary() {
    }

    // java:S1764 — identical expressions on both sides of '==' → always true (a Bug).
    public static boolean alwaysTrue(int value) {
        return value == value;
    }
}
