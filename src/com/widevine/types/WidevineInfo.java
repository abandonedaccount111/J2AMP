package com.widevine.types;

import java.util.Hashtable;
import com.widevine.WidevineConstants;

/**
 * Device/session metadata extracted from the ClientIdentification blob.
 * Equivalent to the WidevineInfo interface in types.ts.
 */
public class WidevineInfo {

    /** Key/value pairs from ClientIdentification.client_info repeated field. */
    public Hashtable clientInfo;   // String -> String

    /** DrmCertificate.system_id */
    public long systemId;

    /** DEVICE_TYPE_CHROME or DEVICE_TYPE_ANDROID */
    public int deviceType;

    /** WVD file version (1 or 2), 0 if not from WVD. */
    public int deviceVersion;

    /** Security level 1-3, 0 if unknown. */
    public int securityLevel;

    public WidevineInfo() {
        clientInfo    = new Hashtable();
        systemId      = 0;
        deviceType    = WidevineConstants.DEVICE_TYPE_CHROME;
        deviceVersion = 0;
        securityLevel = 0;
    }
}
