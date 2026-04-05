package com.amplayer.utils;

import javax.microedition.rms.RecordStore;

/**
 * Persists Apple Music tokens (devToken, userToken) in the device RMS.
 *
 * Record layout inside store "AMTokens":
 *   Record 1 — devToken  (UTF-8 bytes)
 *   Record 2 — userToken (UTF-8 bytes)
 */
public class TokenStore {

    private static final String STORE_NAME = "AMTokens";

    /**
     * Save (or overwrite) both tokens.
     * Deletes the existing store first to guarantee record IDs are predictable.
     */
    public static void save(String devToken, String userToken) throws Exception {
        try { RecordStore.deleteRecordStore(STORE_NAME); } catch (Exception ignored) {}
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE_NAME, true);
            byte[] d = devToken.getBytes("UTF-8");
            byte[] u = userToken.getBytes("UTF-8");
            rs.addRecord(d, 0, d.length);
            rs.addRecord(u, 0, u.length);
        } finally {
            if (rs != null) try { rs.closeRecordStore(); } catch (Exception ignored) {}
        }
    }

    /**
     * Load stored tokens.
     * @return String[]{devToken, userToken}, or null if not stored / unreadable.
     */
    public static String[] load() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE_NAME, false);
            if (rs.getNumRecords() < 2) return null;
            String dev  = new String(rs.getRecord(1), "UTF-8");
            String user = new String(rs.getRecord(2), "UTF-8");
            if (dev.length() == 0 || user.length() == 0) return null;
            return new String[]{ dev, user };
        } catch (Exception e) {
            return null;
        } finally {
            if (rs != null) try { rs.closeRecordStore(); } catch (Exception ignored) {}
        }
    }

    /** Remove stored tokens (e.g. for logout / reset). */
    public static void clear() {
        try { RecordStore.deleteRecordStore(STORE_NAME); } catch (Exception ignored) {}
    }
}
