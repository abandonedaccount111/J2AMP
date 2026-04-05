package com.widevine;

import java.util.Hashtable;
import java.util.Vector;
import com.ponderingpanda.protobuf.*;
import com.google.video.widevine.protos.*;
import com.widevine.types.*;

/**
 * Main entry point for Widevine DRM operations.
 * Ported from index.ts (node-widevine).
 *
 * <h3>Quick-start (raw credentials)</h3>
 * <pre>
 *   byte[] clientIdBlob  = ...; // contents of client_id.bin
 *   byte[] privateKeyDer = ...; // PKCS#8 DER RSA private key
 *   WidevineDRM wv = WidevineDRM.init(clientIdBlob, privateKeyDer);
 *
 *   byte[] pssh = ...;
 *   LicenseSession session = wv.createSession(pssh, WidevineConstants.LICENSE_TYPE_STREAMING);
 *   byte[] challenge = session.generateChallenge();
 *   // POST challenge to license server...
 *   byte[] licenseResponse = ...;
 *   Vector keys = session.parseLicense(licenseResponse);
 * </pre>
 *
 * <h3>Quick-start (WVD file)</h3>
 * <pre>
 *   byte[] wvdBytes = ...; // contents of device.wvd
 *   WidevineDRM wv = WidevineDRM.initWVD(wvdBytes);
 * </pre>
 */
public class WidevineDRM {

    private final byte[]      identifierBlob;
    private final byte[]      privateKeyDer;
    private final WidevineInfo info;

    private WidevineDRM(byte[] identifierBlob, byte[] privateKeyDer, WidevineInfo info) {
        this.identifierBlob = identifierBlob;
        this.privateKeyDer  = privateKeyDer;
        this.info           = info;
    }

    // =========================================================================
    // Factory methods
    // =========================================================================

    /**
     * Initialise from raw device credentials.
     *
     * @param identifierBlob  ClientIdentification protobuf (client_id.bin).
     * @param privateKeyDer   RSA-2048 private key in PKCS#8 DER format.
     * @param deviceType      WidevineConstants.DEVICE_TYPE_CHROME or DEVICE_TYPE_ANDROID.
     * @return ready-to-use WidevineDRM instance.
     */
    public static WidevineDRM init(byte[] identifierBlob, byte[] privateKeyDer, int deviceType) {
        ClientIdentification client_iden = ClientIdentification.fromBytes(identifierBlob);
        DrmCertificate drm_cert = DrmCertificate.fromBytes(client_iden.getToken());
        WidevineInfo info = new WidevineInfo();
        info.deviceType = deviceType;
        info.systemId = drm_cert.getSystemId();
        
        if (client_iden.getClientInfoCount() > 0){
            for (int i = 0; i < client_iden.getClientInfoCount(); i++) {
                ClientIdentification.NameValue ci = client_iden.getClientInfo(i);
                info.clientInfo.put(ci.getName(), ci.getValue());

            }
        }
        
        System.out.println("Device System ID: " + info.systemId);
        System.out.println("Device Type: " + info.deviceType);
        System.out.println("Client Info: " + info.clientInfo);
        System.out.println("DRM Certificate: " + drm_cert.getSystemId());
        System.out.println("Private Key DER size: " + privateKeyDer.length + " bytes");
        
        return new WidevineDRM(identifierBlob, privateKeyDer, info);
    }

    /**
     * Initialise from raw device credentials, auto-detecting device type as CHROME.
     */
    public static WidevineDRM init(byte[] identifierBlob, byte[] privateKeyDer) {
        return init(identifierBlob, privateKeyDer, WidevineConstants.DEVICE_TYPE_CHROME);
    }


    // =========================================================================
    // Session creation
    // =========================================================================

    /**
     * Create a new license acquisition session.
     *
     * @param pssh         PSSH box bytes (or raw WidevinePsshData).
     * @param licenseType  WidevineConstants.LICENSE_TYPE_*.
     * @return new LicenseSession.
     */
    public LicenseSession createSession(byte[] pssh, int licenseType) {
        return new LicenseSession(identifierBlob, privateKeyDer, info.deviceType,
                                  pssh, licenseType);
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    /** Return parsed device/session metadata. */
    public WidevineInfo getInfo() {
        return info;
    }

}
