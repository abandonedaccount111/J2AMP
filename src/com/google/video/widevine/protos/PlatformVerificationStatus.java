package com.google.video.widevine.protos;

public class PlatformVerificationStatus {
    public static final int PLATFORM_UNVERIFIED = 0;
    public static final int PLATFORM_TAMPERED = 1;
    public static final int PLATFORM_SOFTWARE_VERIFIED = 2;
    public static final int PLATFORM_HARDWARE_VERIFIED = 3;
    public static final int PLATFORM_NO_VERIFICATION = 4;
    public static final int PLATFORM_SECURE_STORAGE_SOFTWARE_VERIFIED = 5;
}
