package com.amplayer.utils;

import javax.microedition.rms.RecordStore;
import javax.microedition.media.Manager;

/**
 * Global app settings persisted in RMS store "AMSettings".
 *
 * Record layout:
 *   Record 1  — marqueeEnabled      ("1" or "0")
 *   Record 2  — cacheMb             (int as decimal string, default 20)
 *   Record 3  — lastFmUser          (UTF-8 string, "" if not set)
 *   Record 4  — lastFmSk            (session key, "" if not set)
 *   Record 5  — lastFmNowPlaying    ("1" or "0", default "0")
 *   Record 6  — performanceMode     ("auto", "low", or "normal")
 *   Record 7  — artEnabled          ("1" or "0")
 *   Record 8  — preloadEnabled      ("1" or "0")
 *   Record 9  — bbWifiEnabled       ("1" or "0", BlackBerry WiFi routing)
 *   Record 10 — cjkImageRender      ("1" or "0", render CJK chars via image service)
 *
 * artEnabled and preloadEnabled each have their own row in Settings and can be
 * toggled independently. Performance mode applies presets for both, but an
 * explicit user toggle (records 7/8) always takes precedence after load.
 * queryLimit is derived-only (not persisted).
 *
 *   Record 11 — dbReloadInterval    (int: 0, 1, 5, 10, -1) default 5
 *   Record 12 — maxItemSize         (int: 0=auto, 100, 500, 1000...) default 0
 *   Record 13 — maxQueueSize        (int: 0=auto, 100, 500, 1000...) default 0
 */
public class Settings {

    private static final String STORE = "AMSettings";

    // -------------------------------------------------------------------------
    // BlackBerry detection (evaluated once at class-load time)
    // -------------------------------------------------------------------------

    public static final boolean IS_BLACKBERRY;
    static {
        String platform = System.getProperty("microedition.platform");
        IS_BLACKBERRY = platform != null
                && platform.toLowerCase().startsWith("blackberry");
    }

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

    // Database / Cache settings
    public static int dbReloadInterval = 5;
    public static int maxItemSize      = 0;
    public static int maxQueueSize     = 0;

    // -------------------------------------------------------------------------
    // Derived / runtime flags (not persisted directly)
    // -------------------------------------------------------------------------

    public static boolean lowMemoryMode  = false;
    public static boolean preloadEnabled  = true;   // persisted as record 8
    public static boolean bbWifiEnabled    = false;  // persisted as record 9
    public static boolean cjkImageRender  = false;  // persisted as record 10
    public static int     queryLimit      = 100;    // derived-only
    public static String  audioContentType = getSupportedMp4ContentType(); // derived
    
    public static String getSupportedMp4ContentType() {
        String[] types = Manager.getSupportedContentTypes(null);
        
        if (types != null) {
            for (int i = 0; i < types.length; i++) {
                System.out.println("Supported content types: " + types[i]);
                if (types[i] != null && (types[i].toLowerCase().indexOf("mp4") >= 0 || types[i].toLowerCase().indexOf("m4a") >= 0)) {
                    return types[i];
                }
            }
        }
        return null;
    }

    public static String getAudioContentType(String extension) {
        String[] types = Manager.getSupportedContentTypes(null);
        if (types != null) {
            for (int i = 0; i < types.length; i++) {
                // skip if not start with audio
                if (types[i] != null && types[i].toLowerCase().startsWith("audio")) {
                    if (types[i] != null && (types[i].toLowerCase().indexOf(extension) >= 0)) {
                        return types[i];
                    }
                    // Special case for MP3
                    if (extension.equals("mp3") && types[i].toLowerCase().indexOf("mpeg") >= 0) {
                        return types[i];
                    }
                    // Special case for AAC
                    if (extension.equals("aac") && types[i].toLowerCase().indexOf("mp4") >= 0) {
                        return types[i];
                    }
                }
            }
        }
        return null;
    }
        
    
    public static int getMaxItemSize() {
        if (maxItemSize > 0) return maxItemSize;
        return lowMemoryMode ? 100 : 500;
    }
    
    public static int getMaxQueueSize() {
        if (maxQueueSize > 0) return maxQueueSize;
        return lowMemoryMode ? 100 : 200;
    }

    // -------------------------------------------------------------------------
    // Load / save
    // -------------------------------------------------------------------------
    
      private static boolean hasClass(String s) {
        try {
          Class.forName(s);
          return true;
        } catch (ClassNotFoundException e) {
          return false;
        }
      }
      
      private static boolean hasProperty(String propertyName) {
            return System.getProperty(propertyName) != null;
      }

      public static String getDeviceEnvironment() {
            String platform = System.getProperty("microedition.platform");
            String env = "other";

            // Check Symbian variants
            boolean symbianJrt = platform != null && platform.indexOf("platform=S60") != -1;
            boolean symbian =
                symbianJrt
                    || hasProperty("com.symbian.midp.serversocket.support")
                    || hasProperty("com.symbian.default.to.suite.icon")
                    || hasClass("com.symbian.midp.io.protocol.http.Protocol")
                    || hasClass("com.symbian.lcdjava.io.File");

             

            // Nokia S40 detection
            if (hasClass("com.nokia.mid.impl.isa.jam.Jam")) {
                env = "nokia_s40";
            }
            // Symbian-specific logic
            else if (symbian) {
                if (symbianJrt
                    && platform != null
                    && (platform.indexOf("java_build_version=2.") != -1
                        || platform.indexOf("java_build_version=1.4") != -1)) {
                    // EMC (S60v5+) supports mp3 streaming - keep default URL method
                } else if (hasClass("com.symbian.mmapi.PlayerImpl")) {
                    // UIQ - use InputStream
                    env = "uiq";
                }  else {
                    env = "nokia_s60";
                }
            }
            // J2ME Loader
            else if (hasClass("javax.microedition.shell.MicroActivity")) {
                env = "j2me_loader";
            }
            return env;
        }

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

        // Records 7-9 are explicit user overrides that win over the performance preset.
        if (n >= 7) {
            RecordStore rs2 = null;
            try {
                rs2 = RecordStore.openRecordStore(STORE, false);
                int n2 = rs2.getNumRecords();
                if (n2 >= 7)  artEnabled      = "1".equals(readRec(rs2, 7));
                if (n2 >= 8)  preloadEnabled  = "1".equals(readRec(rs2, 8));
                if (n2 >= 9)  bbWifiEnabled   = "1".equals(readRec(rs2, 9));
                if (n2 >= 10) cjkImageRender  = "1".equals(readRec(rs2, 10));
                if (n2 >= 11) {
                    try { dbReloadInterval = Integer.parseInt(readRec(rs2, 11).trim()); } catch (Exception e) {}
                }
                if (n2 >= 12) {
                    try { maxItemSize = Integer.parseInt(readRec(rs2, 12).trim()); } catch (Exception e) {}
                }
                if (n2 >= 13) {
                    try { maxQueueSize = Integer.parseInt(readRec(rs2, 13).trim()); } catch (Exception e) {}
                }
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
            writeRec(rs, bbWifiEnabled    ? "1" : "0");
            writeRec(rs, cjkImageRender   ? "1" : "0");
            writeRec(rs, String.valueOf(dbReloadInterval));
            writeRec(rs, String.valueOf(maxItemSize));
            writeRec(rs, String.valueOf(maxQueueSize));
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
