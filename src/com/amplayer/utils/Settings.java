package com.amplayer.utils;

import javax.microedition.rms.RecordStore;

/**
 * Global app settings persisted in RMS store "AMSettings".
 *
 * Record layout:
 *   Record 1 — marqueeEnabled  ("1" or "0")
 *   Record 2 — cacheMb         (int as decimal string, default 20)
 *   Record 3 — lastFmUser      (UTF-8 string, "" if not set)
 *   Record 4 — lastFmSk        (session key, "" if not set)
 */
public class Settings {

    private static final String STORE = "AMSettings";

    // In-memory values (loaded once per session)
    public static boolean marqueeEnabled = true;
    public static int     cacheMb        = 20;
    public static String  lastFmUser     = "";
    public static String  lastFmSk       = "";

    // -------------------------------------------------------------------------
    // Load / save
    // -------------------------------------------------------------------------

    /** Load settings from RMS into the static fields. Call once at startup. */
    public static void load() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, false);
            int n = rs.getNumRecords();
            if (n >= 1) marqueeEnabled = !"0".equals(readRec(rs, 1));
            if (n >= 2) {
                try { cacheMb = Integer.parseInt(readRec(rs, 2).trim()); }
                catch (NumberFormatException ignored) {}
            }
            if (n >= 3) lastFmUser = readRec(rs, 3);
            if (n >= 4) lastFmSk   = readRec(rs, 4);
        } catch (Exception ignored) {
            // Store doesn't exist yet — use defaults
        } finally {
            closeQuietly(rs);
        }
    }

    /** Persist all current static field values to RMS. */
    public static void save() {
        try { RecordStore.deleteRecordStore(STORE); } catch (Exception ignored) {}
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            writeRec(rs, marqueeEnabled ? "1" : "0");
            writeRec(rs, String.valueOf(cacheMb));
            writeRec(rs, lastFmUser != null ? lastFmUser : "");
            writeRec(rs, lastFmSk   != null ? lastFmSk   : "");
        } catch (Exception ignored) {
        } finally {
            closeQuietly(rs);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String readRec(RecordStore rs, int id) throws Exception {
        byte[] data = rs.getRecord(id);
        if (data == null || data.length == 0) return "";
        return new String(data, "UTF-8");
    }

    private static void writeRec(RecordStore rs, String value) throws Exception {
        byte[] data = value.getBytes("UTF-8");
        rs.addRecord(data, 0, data.length);
    }

    private static void closeQuietly(RecordStore rs) {
        if (rs != null) try { rs.closeRecordStore(); } catch (Exception ignored) {}
    }
}
