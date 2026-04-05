package com.widevine;

/**
 * Constants for the Widevine DRM protocol.
 * Ported from consts.ts (node-widevine).
 */
public class WidevineConstants {

    // Widevine System ID: edef8ba9-79d6-4ace-a3c8-27dcd51d21ed
    public static final byte[] WIDEVINE_SYSTEM_ID = {
        (byte)0xed, (byte)0xef, (byte)0x8b, (byte)0xa9,
        (byte)0x79, (byte)0xd6, (byte)0x4a, (byte)0xce,
        (byte)0xa3, (byte)0xc8, (byte)0x27, (byte)0xdc,
        (byte)0xd5, (byte)0x1d, (byte)0x21, (byte)0xed
    };

    /**
     * Widevine Root CA Public Key (RSA-2048, SubjectPublicKeyInfo DER).
     * Used to verify service certificate signatures.
     * Copy from the WIDEVINE_ROOT_PUBLIC_KEY constant in node-widevine/src/consts.ts.
     * TODO: Fill in from consts.ts
     */
    public static final byte[] WIDEVINE_ROOT_PUBLIC_KEY = {
        // TODO: copy bytes from WIDEVINE_ROOT_PUBLIC_KEY in consts.ts
    };

    /**
     * Service certificate challenge: a SignedMessage with type=SERVICE_CERTIFICATE (4).
     * Proto encoding: field 1 (type), varint = 4  =>  0x08 0x04
     */
    public static final byte[] SERVICE_CERTIFICATE_CHALLENGE = { 0x08, 0x04 };

    /**
     * Default/common Widevine service certificate (pre-encoded SignedDrmCertificate).
     * Copy from COMMON_SERVICE_CERTIFICATE in consts.ts.
     * TODO: Fill in from consts.ts
     */
    public static final byte[] COMMON_SERVICE_CERTIFICATE = {
        // TODO: copy bytes from COMMON_SERVICE_CERTIFICATE in consts.ts
    };

    // ---- Device types ----
    public static final int DEVICE_TYPE_CHROME  = 1;
    public static final int DEVICE_TYPE_ANDROID = 2;

    // ---- License types (matches LicenseType enum in proto) ----
    public static final int LICENSE_TYPE_STREAMING = 1;
    public static final int LICENSE_TYPE_OFFLINE   = 2;
    public static final int LICENSE_TYPE_AUTOMATIC = 3;

    // ---- SignedMessage.MessageType enum values ----
    public static final int MSG_TYPE_LICENSE_REQUEST       = 1;
    public static final int MSG_TYPE_LICENSE               = 2;
    public static final int MSG_TYPE_ERROR_RESPONSE        = 3;
    public static final int MSG_TYPE_SERVICE_CERTIFICATE   = 4;

    // ---- ClientIdentification.TokenType ----
    public static final int TOKEN_TYPE_KEYBOX              = 0;
    public static final int TOKEN_TYPE_DRM_DEVICE_CERT     = 1;
    public static final int TOKEN_TYPE_REMOTE_ATTESTATION  = 2;
    public static final int TOKEN_TYPE_OEMCRYPTO_API       = 3;

    // ---- ProtocolVersion ----
    public static final int PROTOCOL_VERSION_2_0 = 0x20;  // 2.0
    public static final int PROTOCOL_VERSION_2_1 = 0x21;  // 2.1

    // ---- KeyContainer.KeyType ----
    public static final int KEY_TYPE_SIGNING          = 1;
    public static final int KEY_TYPE_CONTENT          = 2;
    public static final int KEY_TYPE_KEY_CONTROL      = 3;
    public static final int KEY_TYPE_OPERATOR_SESSION = 4;

    // ---- SecurityLevel ----
    public static final int SECURITY_LEVEL_1 = 1; // HW-backed TEE
    public static final int SECURITY_LEVEL_2 = 2;
    public static final int SECURITY_LEVEL_3 = 3; // SW only

    private WidevineConstants() {}
}
