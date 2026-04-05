package com.widevine;

import java.util.Vector;


import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;
import org.bouncycastle.crypto.signers.PSSSigner;
import com.google.video.widevine.protos.*;
import com.widevine.utils.*;
import com.widevine.types.KeyContainer;
import java.util.Random;
/**
 * Manages a single Widevine license acquisition session.
 * Ported from session.ts (node-widevine).
 *
 * Usage:
 *   LicenseSession s = new LicenseSession(identifierBlob, privateKeyDer,
 *                                         deviceType, pssh, licenseType);
 *   byte[] challenge = s.generateChallenge();
 *   // POST challenge to license server...
 *   byte[] licenseResponse = ...;
 *   Vector keys = s.parseLicense(licenseResponse);
 */
public class LicenseSession {

    // ---- Input device credentials ----
    private final byte[] identifierBlob;
    private final byte[] privateKeyDer;
    private final int    deviceType;

    // ---- Content ----
    private final byte[] pssh;
    private final int    licenseType;

    // ---- State ----
    private byte[] serviceCertificate; // SignedDrmCertificate proto bytes, or null
    private byte[] licenseRequestRaw;  // saved so we can derive keys later

    /**
     * Cryptographically seeded PRNG using BC DigestRandomGenerator (SHA-1 based).
     * Avoids any dependency on java.security.SecureRandom (absent from CLDC 1.1).
     */
    private static final DigestRandomGenerator RNG;
    static {
        RNG = new DigestRandomGenerator(new SHA1Digest());
        RNG.addSeedMaterial(System.currentTimeMillis());
    }

    // ---- PSS salt length: SHA-1 output = 20 bytes ----
    private static final int PSS_SALT_LEN = 20;

    /**
     * @param identifierBlob  ClientIdentification protobuf (client_id.bin).
     * @param privateKeyDer   RSA-2048 private key in PKCS#8 DER format.
     * @param deviceType      WidevineConstants.DEVICE_TYPE_*.
     * @param pssh            raw PSSH box bytes (full box or raw WidevinePsshData).
     * @param licenseType     WidevineConstants.LICENSE_TYPE_*.
     */
    public LicenseSession(byte[] identifierBlob, byte[] privateKeyDer,
                          int deviceType, byte[] pssh, int licenseType) {
        this.identifierBlob = identifierBlob;
        this.privateKeyDer  = privateKeyDer;
        this.deviceType     = deviceType;
        this.pssh           = pssh;
        this.licenseType    = licenseType;
    }

    // =========================================================================
    // Service Certificate
    // =========================================================================

    /**
     * Returns the static SERVICE_CERTIFICATE_CHALLENGE bytes to send to the
     * license server to request its service certificate.
     */
    public static byte[] getServiceCertificateChallenge() {
        return WidevineConstants.SERVICE_CERTIFICATE_CHALLENGE;
    }

    /**
     * Configure session to use the default (common) Widevine service certificate.
     * This enables client ID encryption for privacy.
     */
    public void setDefaultServiceCertificate() throws Exception {
        setServiceCertificateFromMessage(WidevineConstants.COMMON_SERVICE_CERTIFICATE);
    }

    /**
     * Parse a SignedMessage response and extract the embedded DrmCertificate,
     * verifying it against the Widevine root public key.
     *
     * @param signedMessageBytes  raw bytes returned by the license server.
     */
    public void setServiceCertificateFromMessage(byte[] signedMessageBytes) throws Exception {
        // Outer: SignedMessage { type(1), msg(2), signature(3) }
        SignedMessage s = SignedMessage.fromBytes(signedMessageBytes);
        int msgType = s.getType();
//        if (msgType != WidevineConstants.MSG_TYPE_SERVICE_CERTIFICATE) {
//            throw new IllegalArgumentException(
//                "Expected SERVICE_CERTIFICATE message type, got: " + msgType);
//        }
        byte[] signedDrmCertBytes = s.getMsg();
        if (signedDrmCertBytes == null) {
            throw new IllegalArgumentException("Missing msg field in service certificate response");
        }
        setServiceCertificate(signedDrmCertBytes);
    }

    /**
     * Set and verify a SignedDrmCertificate.
     *
     * @param signedDrmCertBytes  SignedDrmCertificate protobuf bytes.
     */
    public void setServiceCertificate(byte[] signedDrmCertBytes) throws Exception {
        if (WidevineConstants.WIDEVINE_ROOT_PUBLIC_KEY.length == 0) {
            // Root key not populated — skip verification (development mode).
            this.serviceCertificate = signedDrmCertBytes;
            return;
        }

        // SignedDrmCertificate { drmCertificate(1), signature(2) }
        SignedDrmCertificate sdc = SignedDrmCertificate.fromBytes(signedDrmCertBytes);
        byte[] drmCertBytes = sdc.getDrmCertificate();
        byte[] signature    = sdc.getSignature();

        if (drmCertBytes == null || signature == null) {
            throw new IllegalArgumentException("Invalid SignedDrmCertificate structure");
        }

        // Verify: RSA-PSS(SHA-1) signature of drmCertBytes against Widevine root key
        RSAKeyParameters rootPub =
            RsaKeyUtil.decodePublicKey(WidevineConstants.WIDEVINE_ROOT_PUBLIC_KEY);

        // For verification: PSSSigner(cipher, digest, saltLength)
        PSSSigner signer = new PSSSigner(new RSAEngine(), new SHA1Digest(), PSS_SALT_LEN);
        signer.init(false, rootPub);
        signer.update(drmCertBytes, 0, drmCertBytes.length);
        if (!signer.verifySignature(signature)) {
            throw new SecurityException("Service certificate signature verification failed");
        }

        this.serviceCertificate = signedDrmCertBytes;
    }

    // =========================================================================
    // Challenge Generation
    // =========================================================================

    /**
     * Build and return the Widevine license challenge (a SignedMessage protobuf).
     * POST these bytes to the license server's license acquisition endpoint.
     *
     * @return serialized SignedMessage bytes.
     * @throws Exception on crypto or encoding errors.
     */
    public byte[] generateChallenge() throws Exception {
        // 1. Extract WidevinePsshData payload from the PSSH box
        byte[] psshData = extractWidevinePsshData(pssh);

        // 2. Build LicenseRequest.ContentIdentification.WidevinePsshData
        //    { pssh_data(1), license_type(2), request_id(3) }
        byte[] requestId = generateRequestId();
        LicenseRequest.ContentIdentification.WidevinePsshData ps_data =  
        new LicenseRequest.ContentIdentification.WidevinePsshData();
        ps_data.addPsshData(psshData);
        ps_data.setLicenseType(licenseType);
        ps_data.setRequestId(requestId);
        

        // 3. Build ContentIdentification { ps_data(2) }
        LicenseRequest.ContentIdentification cont_iden = 
                new LicenseRequest.ContentIdentification();
        cont_iden.setWidevinePsshData(ps_data);

        // 4. Build LicenseRequest body
        //    Fields: client_id(1) | encrypted_client_id(9), content_id(2),
        //            type(3), request_id(5), protocol_version(7)
        LicenseRequest licReq = new LicenseRequest();
        licReq.setType(LicenseRequest.NEW);
        licReq.setContentId(cont_iden);
        licReq.setRequestTime(System.currentTimeMillis() / 1000L);

        if (serviceCertificate != null) {
            EncryptedClientIdentification encClientId = encryptClientIdentification();
            licReq.setEncryptedClientId(encClientId);
        } else {
            licReq.setClientId(ClientIdentification.fromBytes(this.identifierBlob));
        }
        
        licReq.setProtocolVersion(ProtocolVersion.VERSION_2_1);
        licReq.setKeyControlNonce(new Random().nextInt(Integer.MAX_VALUE));
       

        licenseRequestRaw = licReq.toBytes();

        // 5. Sign with device RSA private key (RSA-PSS, SHA-1)
        byte[] signature = signRsaPss(licenseRequestRaw);

        // 6. Wrap in SignedMessage { type(1)=LICENSE_REQUEST, msg(2), signature(3) }
        SignedMessage signedMsg = new SignedMessage();
        signedMsg.setType(SignedMessage.LICENSE_REQUEST);
        signedMsg.setMsg(licenseRequestRaw);
        signedMsg.setSignature(signature);

        return signedMsg.toBytes();
    }

    // =========================================================================
    // License Parsing / Key Extraction
    // =========================================================================

    /**
     * Parse a license response from the license server and return decrypted keys.
     *
     * @param licenseResponseBytes  raw bytes returned by the license server.
     * @return Vector of KeyContainer (never null; may be empty).
     * @throws Exception on crypto or protocol errors.
     */
    public Vector parseLicense(byte[] licenseResponseBytes) throws Exception {
        if (licenseRequestRaw == null) {
            throw new IllegalStateException(
                "generateChallenge() must be called before parseLicense()");
        }

        // Outer: SignedMessage { type(1), msg(2), signature(3), session_key(4) }
        SignedMessage signedMsg = SignedMessage.fromBytes(licenseResponseBytes);

        byte[] encryptedSessionKey = signedMsg.getSessionKey();
        byte[] licenseBytes        = signedMsg.getMsg();
        byte[] msgSignature        = signedMsg.getSignature();

        if (encryptedSessionKey == null || licenseBytes == null) {
            throw new IllegalArgumentException(
                "Invalid license response: missing session_key or msg");
        }

        // 1. Decrypt session key with device RSA private key (RSA-OAEP, SHA-1)
        byte[] sessionKey = decryptRsaOaep(encryptedSessionKey);

        // 2. Derive encryption and authentication sub-keys via AES-CMAC
        //    encKey  = CMAC(sessionKey, "ENCRYPTION"     || 0x00 || req || LE32(0x80))
        //    authKey = CMAC(sessionKey, "AUTHENTICATION" || 0x00 || req || LE32(0x200)) × 2 blocks
        byte[] encKey  = deriveKey(sessionKey, "ENCRYPTION",     licenseRequestRaw, 0x80);
        byte[] authKey = deriveKey(sessionKey, "AUTHENTICATION", licenseRequestRaw, 0x200);

        // 3. Verify HMAC-SHA256 of the license message
        if (msgSignature != null) {
            byte[] expectedMac = hmacSha256(authKey, licenseBytes);
            if (!ByteUtils.equals(expectedMac, msgSignature)) {
                throw new SecurityException("License response HMAC-SHA256 verification failed");
            }
        }

        // 4. Decrypt content keys from License.key[] repeated field
        //    License { id(1), policy(2), key(3)[] }
        License license = License.fromBytes(licenseBytes);
        Vector keyEntries = license.getKeyVector();  // repeated KeyContainer
        Vector result = new Vector();
        
        System.out.println("Key size: "+ keyEntries.size());

        for (int i = 0; i < keyEntries.size(); i++) {
            License.KeyContainer kc = (License.KeyContainer) keyEntries.elementAt(i); 
            byte[] kid  = kc.getId();
            byte[] iv   = kc.getIv();
            byte[] encK = kc.getKey();
            int    type = kc.getType();

            if (iv == null || encK == null) continue;

            // Decrypt key with AES-128-CBC using derived encryption key
            byte[] decryptedKey = aesCbcDecrypt(encK, encKey, iv);
            String kidHex = (kid != null) ? ByteUtils.toHex(kid) : "";
            if (kid != null) {
                result.addElement(new KeyContainer(kidHex, ByteUtils.toHex(decryptedKey), type));
            }
        }

        return result;
    }

    // =========================================================================
    // Private helpers — crypto
    // =========================================================================

    /**
     * RSA-OAEP (SHA-1) decrypt using device private key.
     * Throws InvalidCipherTextException if decryption fails.
     */
    private byte[] decryptRsaOaep(byte[] data) throws Exception {
        RSAPrivateCrtKeyParameters privKey = RsaKeyUtil.decodePrivateKey(privateKeyDer);
        // OAEPEncoding(cipher, hashForOAEP) — SHA-1 per Widevine spec
        OAEPEncoding cipher = new OAEPEncoding(new RSAEngine(), new SHA1Digest());
        cipher.init(false, privKey);
        // processBlock throws InvalidCipherTextException on bad padding
        return cipher.processBlock(data, 0, data.length);
    }

    /**
     * RSA-PSS (SHA-1, 20-byte salt) sign using device private key.
     *
     * Uses PSSSigner(cipher, digest, byte[] salt) constructor to supply
     * the salt directly — avoids any dependency on java.security.SecureRandom
     * which is not guaranteed in CLDC 1.1.
     */
    private byte[] signRsaPss(byte[] data) throws CryptoException, Exception {
        RSAPrivateCrtKeyParameters privKey = RsaKeyUtil.decodePrivateKey(privateKeyDer);

        //        // Generate random salt using BC's DigestRandomGenerator (no SecureRandom needed)
        //        byte[] salt = new byte[PSS_SALT_LEN];
        //        RNG.nextBytes(salt);

        // PSSSigner(cipher, contentDigest, salt) — salt is pre-provided
        PSSSigner signer = new PSSSigner(new RSAEngine(), new SHA1Digest(), PSS_SALT_LEN);
        signer.init(true, privKey);
        signer.update(data, 0, data.length);
        // generateSignature() throws CryptoException on error
        return signer.generateSignature();
    }

    /**
     * Derive a Widevine sub-key from the RSA-decrypted session key via AES-CMAC.
     *
     * The construction mirrors the TypeScript implementation:
     *   counter(1 byte) || label || 0x00 || licenseRequest || counterMax(4 bytes BE)
     *
     * For ENCRYPTION  → 1 CMAC block  → 16 bytes
     * For AUTHENTICATION → 2 CMAC blocks → 32 bytes (counter 1 then 2, concatenated)
     */
    private byte[] deriveKey(byte[] sessionKey, String label,
                             byte[] requestBytes, int counterMax) {
        AesCmac cmac = new AesCmac(sessionKey);

        int iterations = (counterMax == 0x200) ? 2 : 1;
        byte[][] parts = new byte[iterations][];

        byte[] labelBytes;
        try { labelBytes = label.getBytes("ASCII"); }
        catch (Exception e) { labelBytes = label.getBytes(); }

        byte[] nullByte = { 0x00 };
        byte[] maxBytes = ByteUtils.int32BE(counterMax);

        for (int counter = 1; counter <= iterations; counter++) {
            byte[] counterByte = { (byte) counter };

            // Build:  counter || label || \0 || requestBytes || counterMax
            byte[] msg = ByteUtils.concat3(
                ByteUtils.concat(counterByte, labelBytes),
                ByteUtils.concat(nullByte, requestBytes),
                maxBytes
            );
            parts[counter - 1] = cmac.calculate(msg);
        }

        return (iterations == 1) ? parts[0] : ByteUtils.concat(parts[0], parts[1]);
    }

    /** HMAC-SHA256. */
    private byte[] hmacSha256(byte[] key, byte[] data) {
        HMac hmac = new HMac(new SHA256Digest());
        hmac.init(new KeyParameter(key));
        hmac.update(data, 0, data.length);
        byte[] out = new byte[hmac.getMacSize()];
        hmac.doFinal(out, 0);
        return out;
    }

    /**
     * AES-128-CBC decrypt.
     * PaddedBufferedBlockCipher.doFinal throws InvalidCipherTextException on bad padding.
     */
    private byte[] aesCbcDecrypt(byte[] ciphertext, byte[] key, byte[] iv)
            throws InvalidCipherTextException {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
            new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
        cipher.init(false, new ParametersWithIV(new KeyParameter(key), iv));

        byte[] out = new byte[cipher.getOutputSize(ciphertext.length)];
        int len = cipher.processBytes(ciphertext, 0, ciphertext.length, out, 0);
        len += cipher.doFinal(out, len);
        return (len < out.length) ? ByteUtils.slice(out, 0, len) : out;
    }

    // =========================================================================
    // Private helpers — EncryptedClientIdentification
    // =========================================================================

    /**
     * Encrypt the ClientIdentification blob with the service certificate RSA public key.
     *
     * Returns EncryptedClientIdentification {
     *   provider_id(1), service_certificate_serial_number(2),
     *   encrypted_client_id(3), encrypted_client_id_iv(4), encrypted_privacy_key(5)
     * }
     */
    private EncryptedClientIdentification encryptClientIdentification() throws Exception {
        // Parse SignedDrmCertificate { drmCertificate(1) }
        SignedDrmCertificate sdc = SignedDrmCertificate.fromBytes(serviceCertificate);
        byte[] drmCertBytes = sdc.getDrmCertificate();
        if (drmCertBytes == null) {
            throw new IllegalStateException("Bad service certificate: missing drmCertificate");
        }

        // DrmCertificate { serial_number(1), ..., public_key(5), ..., provider_id(8) }
        DrmCertificate cert = DrmCertificate.fromBytes(drmCertBytes);
        byte[] serialNumber = cert.getSerialNumber();
        byte[] publicKeyDer = cert.getPublicKey(); // SubjectPublicKeyInfo DER
        String providerId   = cert.getProviderId();
        if (providerId == null) providerId = "";

        if (publicKeyDer == null) {
            throw new IllegalStateException("Service certificate missing public_key");
        }

        // Generate random AES-128 key and IV using BC PRNG
        byte[] aesKey = new byte[16];
        byte[] aesIv  = new byte[16];
        RNG.nextBytes(aesKey);
        RNG.nextBytes(aesIv);

        // Encrypt identifierBlob with AES-128-CBC
        PaddedBufferedBlockCipher aesCipher = new PaddedBufferedBlockCipher(
            new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
        aesCipher.init(true, new ParametersWithIV(new KeyParameter(aesKey), aesIv));
        byte[] encClientId = new byte[aesCipher.getOutputSize(identifierBlob.length)];
        int len = aesCipher.processBytes(identifierBlob, 0, identifierBlob.length,
                                          encClientId, 0);
        len += aesCipher.doFinal(encClientId, len);
        if (len < encClientId.length) encClientId = ByteUtils.slice(encClientId, 0, len);

        // Encrypt aesKey with the service certificate RSA public key (RSA-OAEP, SHA-1)
        RSAKeyParameters svcPub = RsaKeyUtil.decodePublicKey(publicKeyDer);
        OAEPEncoding rsaCipher = new OAEPEncoding(new RSAEngine(), new SHA1Digest());
        rsaCipher.init(true, svcPub);
        byte[] encPrivacyKey = rsaCipher.processBlock(aesKey, 0, aesKey.length);

        // Assemble EncryptedClientIdentification
        EncryptedClientIdentification enc = new EncryptedClientIdentification();
        enc.setProviderId(providerId);
        if (serialNumber != null) enc.setServiceCertificateSerialNumber(serialNumber);
        enc.setEncryptedClientId(encClientId);
        enc.setEncryptedClientIdIv(aesIv);
        enc.setEncryptedPrivacyKey(encPrivacyKey);
        return enc;
    }

    // =========================================================================
    // Private helpers — PSSH / request ID
    // =========================================================================

    /**
     * Extract the WidevinePsshData payload from a full PSSH box.
     *
     * PSSH box layout (ISO 14496-12):
     *   4B size | 4B "pssh" | 1B version | 3B flags | 16B systemID
     *   | [KID list if v1] | 4B dataSize | N bytes WidevinePsshData
     *
     * If the input doesn't look like a PSSH box, it is returned as-is
     * (assumed to be raw WidevinePsshData).
     */
    private byte[] extractWidevinePsshData(byte[] psshBox) {
        int off = 0;
        while (off + 32 <= psshBox.length) {
            long boxSize = ByteUtils.readUint32BE(psshBox, off);
            if (psshBox[off+4]=='p' && psshBox[off+5]=='s'
             && psshBox[off+6]=='s' && psshBox[off+7]=='h') {
                byte[] sysId = ByteUtils.slice(psshBox, off + 12, off + 28);
                if (ByteUtils.equals(sysId, WidevineConstants.WIDEVINE_SYSTEM_ID)) {
                    long dataSize = ByteUtils.readUint32BE(psshBox, off + 28);
                    return ByteUtils.slice(psshBox, off + 32,
                                           (int)(off + 32 + dataSize));
                }
            }
            if (boxSize < 8) break;
            off += (int) boxSize;
        }
        // Not a box or Widevine box not found — return as raw WidevinePsshData
        return psshBox;
    }

    /**
     * Generate a random request ID.
     * Android: 16 random bytes.
     * Chrome:  32-character uppercase hex string as ASCII bytes.
     */
    private byte[] generateRequestId() {
        byte[] id = new byte[16];
        RNG.nextBytes(id);
        if (deviceType == WidevineConstants.DEVICE_TYPE_ANDROID) {
            return id;
        }
        // Chrome-style: uppercase hex ASCII
        String hex = ByteUtils.toHex(id).toUpperCase();
        try { return hex.getBytes("ASCII"); }
        catch (Exception e) { return hex.getBytes(); }
    }
}
