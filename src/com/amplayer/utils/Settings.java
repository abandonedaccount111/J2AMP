package com.amplayer.utils;

import javax.microedition.rms.RecordStore;

/**
 * Global app settings persisted in RMS store "AMSettings".
 *
 * Record layout:
 *   Record 1 — marqueeEnabled      ("1" or "0")
 *   Record 2 — cacheMb             (int as decimal string, default 20)
 *   Record 3 — lastFmUser          (UTF-8 string, "" if not set)
 *   Record 4 — lastFmSk            (session key, "" if not set)
 *   Record 5 — lastFmNowPlaying    ("1" or "0", default "0")
 *   Record 6 — performanceMode     ("auto", "low", or "normal")
 *   Record 7 — artEnabled          ("1" or "0")
 *   Record 8 — preloadEnabled      ("1" or "0")
 *
 * artEnabled and preloadEnabled each have their own row in Settings and can be
 * toggled independently. Performance mode applies presets for both, but an
 * explicit user toggle (records 7/8) always takes precedence after load.
 * queryLimit is derived-only (not persisted).
 */
public class Settings {

    private static final String STORE = "AMSettings";

    // -------------------------------------------------------------------------
    // Persisted fields
    // -------------------------------------------------------------------------

    public static boolean marqueeEnabled   = true;
    public static int     cacheMb          = 20;
    public static String  lastFmUser       = "";
    public static String  lastFmSk         = "";
    public static boolean lastFmNowPlaying = false;
    /** "auto" | "low" | "normal" */
    public static String  performanceMode  = "auto";
    public static boolean artEnabled       = true;

    // -------------------------------------------------------------------------
    // Derived / runtime flags (not persisted directly)
    // -------------------------------------------------------------------------

    public static boolean lowMemoryMode  = false;
    public static boolean preloadEnabled = true;  // persisted as record 8
    public static int     queryLimit     = 100;   // derived-only

    // -------------------------------------------------------------------------
    // Load / save
    // -------------------------------------------------------------------------

    /** Load persisted settings. Call once at startup. */
    public static void load() {
        RecordStore rs = null;
        int n = 0;
        try {
            rs = RecordStore.openRecordStore(STORE, false);
            n = rs.getNumRecords();
            if (n >= 1) marqueeEnabled   = !"0".equals(readRec(rs, 1));
            if (n >= 2) {
                try { cacheMb = Integer.parseInt(readRec(rs, 2).trim()); }
                catch (NumberFormatException ignored) {}
            }
            if (n >= 3) lastFmUser       = readRec(rs, 3);
            if (n >= 4) lastFmSk         = readRec(rs, 4);
            if (n >= 5) lastFmNowPlaying = "1".equals(readRec(rs, 5));
            if (n >= 6) performanceMode  = readRec(rs, 6);
        } catch (Exception ignored) {
        } finally {
            closeQuietly(rs);
        }

        // Performance mode sets derived flags (artEnabled, preloadEnabled, etc.) as presets.
        applyPerformanceMode();

        // Records 7 and 8 are explicit user overrides that win over the performance preset.
        if (n >= 7 || n >= 8) {
            RecordStore rs2 = null;
            try {
                rs2 = RecordStore.openRecordStore(STORE, false);
                int n2 = rs2.getNumRecords();
                if (n2 >= 7) artEnabled     = "1".equals(readRec(rs2, 7));
                if (n2 >= 8) preloadEnabled = "1".equals(readRec(rs2, 8));
            } catch (Exception ignored) {
            } finally {
                closeQuietly(rs2);
            }
        }
    }

    /** Persist all current persisted-field values to RMS. */
    public static void save() {
        try { RecordStore.deleteRecordStore(STORE); } catch (Exception ignored) {}
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            writeRec(rs, marqueeEnabled   ? "1" : "0");
            writeRec(rs, String.valueOf(cacheMb));
            writeRec(rs, lastFmUser       != null ? lastFmUser : "");
            writeRec(rs, lastFmSk         != null ? lastFmSk   : "");
            writeRec(rs, lastFmNowPlaying ? "1" : "0");
            writeRec(rs, performanceMode  != null ? performanceMode : "auto");
            writeRec(rs, artEnabled       ? "1" : "0");
            writeRec(rs, preloadEnabled   ? "1" : "0");
        } catch (Exception ignored) {
        } finally {
            closeQuietly(rs);
        }
    }

    // -------------------------------------------------------------------------
    // Performance mode
    // -------------------------------------------------------------------------

    /**
     * Apply the current performanceMode preset.
     * Sets lowMemoryMode, artEnabled (preset), preloadEnabled, queryLimit.
     * Does NOT save — call save() separately if you want to persist.
     */
    public static void applyPerformanceMode() {
        boolean low;
        if ("low".equals(performanceMode))        low = true;
        else if ("normal".equals(performanceMode)) low = false;
        else                                       low = probeIsLowMemory();

        lowMemoryMode  = low;
        artEnabled     = !low;    // preset; user can override this after applyPerformanceMode()
        preloadEnabled = !low;
        queryLimit     = low ? 25 : 100;
    }

    private static boolean probeIsLowMemory() {
        System.gc();
        return Runtime.getRuntime().freeMemory() < 4L * 1024 * 1024;
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
