package com.amplayer.utils;

import com.nokia.mid.iapinfo.AccessPoint;
import javax.microedition.rms.RecordStore;
import com.nokia.mid.iapinfo.IAPInfo;


/**
 * Remembers the user's IAP (Internet Access Point) selection so the
 * "Select connection" dialog appears only once.
 *
 * How it works
 * ────────────
 * On Nokia S60, appending  ;nokia_apnid=<id>  to a GCF connector URL
 * (socket:// or ssl://) bypasses the per-connection IAP dialog.
 *
 * The first time a connection is opened we let the OS show its normal
 * "Select connection" dialog.  Right after the socket is opened we read
 * System.getProperty("com.nokia.network.access"), which S60 sets to a
 * string like "APN:2" or "WLan:0" once a bearer is active.  We parse
 * the numeric suffix as the IAP ID and save it to an RMS record.
 *
 * Every subsequent Connector.open() call — within the same session or
 * in future launches — gets the IAP appended via appendTo(), so the
 * dialog never appears again unless the stored IAP is deleted.
 *
 * Usage (in SocketHttpConnection or any Connector.open site)
 * ──────────────────────────────────────────────────────────
 *   String url = IAPManager.appendTo("socket://host:80");
 *   SocketConnection sc = (SocketConnection) Connector.open(url);
 *   IAPManager.captureFromSystem();   // call once after open succeeds
 */
public class IAPManager {

    private static final String RMS_NAME = "ampiap";
    private static int cachedIap = -1;   // -1 = not yet known

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Appends ";nokia_apnid=N" to a GCF URL when an IAP is already stored.
     * Returns the URL unchanged on the very first run (no IAP known yet).
     */
    public static String appendTo(String url) {
        int id = load();
        System.out.println("CachedIAPID: " + id);
        if (id < 0) return url;
        return url + ";nokia_apnid=" + id;
    }

    public static void captureFromSystem() {
        if (cachedIap >= 0) return;   // already known, nothing to do
        try{
            try {
//            String prop = System.getProperty("com.nokia.network.access");
//            // Expected formats: "APN:2", "WLan:0", "GPRS:4", etc.
//            if (prop == null || prop.length() == 0) return;
//            int colon = prop.lastIndexOf(':');
//            if (colon < 0) return;
//            int id = Integer.parseInt(prop.substring(colon + 1).trim());
//            if (id >= 0) save(id);
            IAPInfo iap_if = IAPInfo.getIAPInfo();
            AccessPoint lastSelectedAccessPoint = iap_if.getLastUsedAccessPoint();
            if (lastSelectedAccessPoint == null) lastSelectedAccessPoint = iap_if.getAccessPoints()[0];
            if (lastSelectedAccessPoint != null) save(lastSelectedAccessPoint.getID());
            System.out.println("IAPID: " + lastSelectedAccessPoint.getID());
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
            
        } catch (Error ignored2) {} 
    }

    /** Forget the stored IAP (forces the dialog to appear again next launch). */
    public static synchronized void reset() {
        cachedIap = -1;
        try { RecordStore.deleteRecordStore(RMS_NAME); } catch (Exception ignored) {}
    }

    // -------------------------------------------------------------------------
    // RMS persistence
    // -------------------------------------------------------------------------

    /** Returns the stored IAP ID, or -1 if none is stored. */
    public static synchronized int load() {
        if (cachedIap >= 0) return cachedIap;
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RMS_NAME, false);
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                if (data != null && data.length >= 2)
                    cachedIap = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
            }
        } catch (Exception ignored) {
        } finally {
            if (rs != null) try { rs.closeRecordStore(); } catch (Exception ignored) {}
        }
        return cachedIap;
    }

    private static synchronized void save(int id) {
        if (id == cachedIap) return;
        cachedIap = id;
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RMS_NAME, true);
            byte[] data = { (byte)(id >> 8), (byte)(id & 0xFF) };
            if (rs.getNumRecords() > 0) {
                rs.setRecord(1, data, 0, data.length);
            } else {
                rs.addRecord(data, 0, data.length);
            }
        } catch (Exception ignored) {
        } finally {
            if (rs != null) try { rs.closeRecordStore(); } catch (Exception ignored) {}
        }
    }
}
