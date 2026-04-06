package com.amplayer.playback;

import com.amplayer.api.AMAPI;
import com.amplayer.utils.Settings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;

/**
 * Manages a playback queue and a single JSR-135 Player.
 *
 * Credentials (clientIdBlob, privateKeyDer) and an AMAPI instance are supplied
 * at construction time and reused for every track.
 *
 * All playback operations that block (decryption, network) are run on a
 * background thread.  UI updates are delivered via the Listener interface.
 */
public class PlaybackManager implements PlayerListener {

    // -------------------------------------------------------------------------
    // Listener
    // -------------------------------------------------------------------------

    public interface Listener {
        /** Called immediately when the current track index changes. */
        void onTrackChanged(int index);
        /** Called when playback starts or stops (including after loading). */
        void onPlayStateChanged(boolean playing);
        /** Called when decryption / network fails. */
        void onError(String msg);
    }

    // -------------------------------------------------------------------------
    // Repeat / shuffle constants
    // -------------------------------------------------------------------------

    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_ONE  = 1;
    public static final int REPEAT_ALL  = 2;

    // -------------------------------------------------------------------------
    // Queue
    // -------------------------------------------------------------------------

    private String[] trackIds;
    private String[] trackNames;
    private String[] trackArtists;
    private String   artUrlTemplate;
    private int      currentIndex = -1;

    // -------------------------------------------------------------------------
    // Shuffle state
    // -------------------------------------------------------------------------

    private boolean shuffle         = false;
    private int[]   shuffledIndices = null;
    private int     shufflePos      = 0;

    // -------------------------------------------------------------------------
    // Repeat state
    // -------------------------------------------------------------------------

    private int repeatMode = REPEAT_NONE;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private Player  player;
    private boolean isPlaying    = false;
    private boolean isLoading    = false;
    private long    pausedTimeUs = -1L;  // media time (µs) saved on pause

    // -------------------------------------------------------------------------
    // Credentials / API  (set lazily via setCredentials before first play)
    // -------------------------------------------------------------------------

    private byte[] clientIdBlob;
    private byte[] privateKeyDer;
    private AMAPI  api;

    private Listener listener;

    // -------------------------------------------------------------------------
    // Preload cache (JSR-75 file system)
    // -------------------------------------------------------------------------

    /** Resolved once on first use; null means no writable root found. */
    private static String  cacheDir         = null;
    private static boolean cacheDirResolved = false;

    private static long MAX_CACHE_BYTES = 20L * 1024 * 1024; // 20 MB default

    /** Update the cache size limit at runtime. Call after loading Settings. */
    public static void setCacheSize(int mb) {
        if (mb < 1)   mb = 1;
        if (mb > 200) mb = 200;
        MAX_CACHE_BYTES = (long) mb * 1024 * 1024;
    }

    /** id → Long(file size in bytes) for every file currently on disk. */
    private static final Hashtable cachedSizes = new Hashtable();
    /** Insertion-order queue for FIFO eviction — oldest entry at index 0. */
    private static final Vector    cacheQueue  = new Vector();
    private static long totalCachedBytes = 0L;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** No-credential constructor — call setCredentials() before first playback. */
    public PlaybackManager() {}

    public PlaybackManager(byte[] clientIdBlob, byte[] privateKeyDer, AMAPI api) {
        this.clientIdBlob  = clientIdBlob;
        this.privateKeyDer = privateKeyDer;
        this.api           = api;
    }

    /** Sets credentials once; ignored if already set. */
    public synchronized void setCredentials(byte[] clientId, byte[] privKey, AMAPI amapi) {
        if (this.api == null) {
            this.clientIdBlob  = clientId;
            this.privateKeyDer = privKey;
            this.api           = amapi;
        }
    }

    public synchronized boolean hasCredentials() { return api != null; }

    // -------------------------------------------------------------------------
    // Queue management
    // -------------------------------------------------------------------------

    public synchronized void setListener(Listener l) { this.listener = l; }
    public synchronized Listener getListener()       { return listener;   }

    /**
     * Replace the entire queue and start playback at {@code startIndex}.
     * {@code artUrlTemplate} is the album / playlist cover URL template
     * (may contain {w}, {h}, {f}).
     */
    public void setQueue(String[] ids, String[] names, String[] artists,
                         String artUrl, int startIndex) {
        trackIds       = ids;
        trackNames     = names;
        trackArtists   = artists;
        artUrlTemplate = artUrl;
        play(startIndex);
    }

    /**
     * Insert one or more tracks immediately after the current position (Play Next).
     * Invalidates shuffle state.
     */
    public synchronized void insertNext(String[] ids, String[] names, String[] artists) {
        if (trackIds == null || trackIds.length == 0) {
            setQueue(ids, names, artists, artUrlTemplate != null ? artUrlTemplate : "", 0);
            return;
        }
        int at    = currentIndex + 1;
        trackIds     = insertArray(trackIds,     ids,     at);
        trackNames   = insertArray(trackNames,   names,   at);
        trackArtists = insertArray(trackArtists, artists, at);
        shuffle         = false;
        shuffledIndices = null;
    }

    /**
     * Append one or more tracks to the end of the queue (Add to Queue / Play Last).
     * Invalidates shuffle state.
     */
    public synchronized void appendToQueue(String[] ids, String[] names, String[] artists) {
        if (trackIds == null || trackIds.length == 0) {
            setQueue(ids, names, artists, artUrlTemplate != null ? artUrlTemplate : "", 0);
            return;
        }
        trackIds     = appendArray(trackIds,     ids);
        trackNames   = appendArray(trackNames,   names);
        trackArtists = appendArray(trackArtists, artists);
        shuffle         = false;
        shuffledIndices = null;
    }

    private static String[] insertArray(String[] base, String[] ins, int at) {
        String[] r = new String[base.length + ins.length];
        System.arraycopy(base, 0,   r, 0,                   at);
        System.arraycopy(ins,  0,   r, at,                  ins.length);
        System.arraycopy(base, at,  r, at + ins.length,     base.length - at);
        return r;
    }

    private static String[] appendArray(String[] base, String[] app) {
        String[] r = new String[base.length + app.length];
        System.arraycopy(base, 0, r, 0,            base.length);
        System.arraycopy(app,  0, r, base.length,  app.length);
        return r;
    }

    // -------------------------------------------------------------------------
    // Playback control
    // -------------------------------------------------------------------------

    public void play(final int index) {
        if (trackIds == null || index < 0 || index >= trackIds.length) return;
        synchronized (this) {
            if (!hasCredentials()) {
                fireError("No credentials — call setCredentials() before playback");
                return;
            }
            if (isLoading) return;
            isLoading    = true;
            currentIndex = index;
        }
        pausedTimeUs = -1L;
        stopPlayer();
        fireTrackChanged(index);

        final String   id      = trackIds[index];
        final String[] idSnap  = trackIds;  // snapshot for preload after play starts
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Try to play from cache file first (avoids RAM for byte array)
                    String cachePath = cacheFilePath(id);
                    if (!cachedSizes.containsKey(id)) {
                        AMSongHelper helper = new AMSongHelper();
                        String songUrl = helper.getWebPlaybackURL(
                            id, "songs",
                            api.getDeveloperToken(), api.getUserToken());
                        byte[] data = helper.getAMDecryptedSong(
                            songUrl, null, clientIdBlob, privateKeyDer,
                            api.getDeveloperToken(), api.getUserToken());
                        if (data == null) throw new Exception("Decryption returned null");
                        writeToCache(id, data);
                        data = null; // free before opening file — peak RAM released
                        System.gc();
                    }
                    // Attempt to play from file stream (saves ~8 MB vs byte-array path)
                    boolean started = startPlaybackFromFile(cachePath);
                    if (!started) {
                        // Fallback: read into byte array (older devices)
                        byte[] data = readFromCache(id);
                        if (data == null) throw new Exception("Cache read failed");
                        startPlayback(data);
                    }
                    evictStale(index, idSnap);
                    schedulePreloads(index, idSnap);
                } catch (Exception e) {
                    synchronized (PlaybackManager.this) { isLoading = false; }
                    fireError(e.getMessage() != null ? e.getMessage() : e.toString());
                }
            }
        }).start();
    }

    /**
     * Try to start playback by streaming directly from a cache file.
     * Returns true on success, false if the device does not support it
     * (caller should fall back to the byte-array path).
     *
     * Playing from an InputStream rather than a ByteArrayInputStream means
     * the decrypted byte array can be released before playback starts,
     * saving ~8 MB of sustained heap usage.
     */
    private synchronized boolean startPlaybackFromFile(String path) {
        if (path == null) return false;
        FileConnection fc = null;
        InputStream    in = null;
        try {
            stopPlayer();
            fc = (FileConnection) Connector.open(path, Connector.READ);
            if (!fc.exists() || fc.fileSize() == 0) return false;
            in     = fc.openInputStream();
            player = Manager.createPlayer(in, Settings.getSupportedMp4ContentType());
            player.realize();
            player.prefetch();
            VolumeControl vc = (VolumeControl) player.getControl("VolumeControl");
            if (vc != null) vc.setLevel(100);
            player.addPlayerListener(this);
            player.start();
            isPlaying = true;
            isLoading = false;
            // Note: fc and in must stay open while the player reads the stream.
            // We intentionally do NOT close them here.
            firePlayStateChanged(true);
            return true;
        } catch (Exception e) {
            // Device may not support InputStream-based player — fall back
            try { if (in != null) in.close(); }   catch (Exception ignored) {}
            try { if (fc != null) fc.close(); }   catch (Exception ignored) {}
            return false;
        }
    }

    private synchronized void startPlayback(byte[] data) throws Exception {
        stopPlayer();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        player = Manager.createPlayer(bais, Settings.getSupportedMp4ContentType());
        player.realize();
        player.prefetch();
        VolumeControl vc = (VolumeControl) player.getControl("VolumeControl");
        if (vc != null) vc.setLevel(100);
        player.addPlayerListener(this);
        player.start();
        isPlaying = true;
        isLoading = false;
        firePlayStateChanged(true);
    }

    public synchronized void pause() {
        if (player != null && isPlaying) {
            try {
                long t = player.getMediaTime();
                pausedTimeUs = (t == Player.TIME_UNKNOWN) ? -1L : t;
                player.stop();
                isPlaying = false;
            } catch (Exception ignored) {}
            firePlayStateChanged(false);
        }
    }

    public synchronized void resume() {
        if (player != null && !isPlaying && !isLoading) {
            try {
                player.start();
                if (pausedTimeUs >= 0) {
                    try { player.setMediaTime(pausedTimeUs); } catch (Exception ignored) {}
                    pausedTimeUs = -1L;
                }
                isPlaying = true;
            } catch (Exception ignored) {}
            firePlayStateChanged(true);
        }
    }

    public void next() {
        if (trackIds == null) return;
        if (shuffle && shuffledIndices != null) {
            if (shufflePos < shuffledIndices.length - 1) {
                play(shuffledIndices[++shufflePos]);
            } else if (repeatMode == REPEAT_ALL) {
                shufflePos = 0;
                play(shuffledIndices[shufflePos]);
            }
        } else {
            if (currentIndex < trackIds.length - 1) {
                play(currentIndex + 1);
            } else if (repeatMode == REPEAT_ALL) {
                play(0);
            }
        }
    }

    public void previous() {
        if (trackIds == null) return;
        if (shuffle && shuffledIndices != null) {
            if (shufflePos > 0) play(shuffledIndices[--shufflePos]);
        } else {
            if (currentIndex > 0) play(currentIndex - 1);
        }
    }

    public synchronized void toggleShuffle() {
        shuffle = !shuffle;
        if (shuffle && trackIds != null) generateShuffle();
    }

    public synchronized void cycleRepeat() {
        repeatMode = (repeatMode + 1) % 3;
    }

    public synchronized boolean isShuffled()  { return shuffle; }
    public synchronized int     getRepeatMode() { return repeatMode; }

    private void generateShuffle() {
        int n = trackIds.length;
        shuffledIndices = new int[n];
        for (int i = 0; i < n; i++) shuffledIndices[i] = i;
        java.util.Random rng = new java.util.Random();
        for (int i = n - 1; i > 0; i--) {
            int j = Math.abs(rng.nextInt() % (i + 1));
            int t = shuffledIndices[i]; shuffledIndices[i] = shuffledIndices[j]; shuffledIndices[j] = t;
        }
        // Move the currently playing track to position 0 so it doesn't replay immediately
        for (int i = 0; i < n; i++) {
            if (shuffledIndices[i] == currentIndex) {
                int t = shuffledIndices[0]; shuffledIndices[0] = shuffledIndices[i]; shuffledIndices[i] = t;
                break;
            }
        }
        shufflePos = 0;
    }

    /** Seek to an absolute position (milliseconds). */
    public synchronized void seekTo(long ms) {
        if (player == null) return;
        try { player.setMediaTime(ms * 1000L); } catch (Exception ignored) {}
    }

    /** Seek relative to the current position (milliseconds, may be negative). */
    public void seekBy(long deltaMs) {
        long pos = getMediaTimeMs() + deltaMs;
        if (pos < 0) pos = 0;
        long dur = getDurationMs();
        if (dur > 0 && pos > dur) pos = dur;
        seekTo(pos);
    }

    public void close() { stopPlayer(); clearCache(); }

    private synchronized void stopPlayer() {
        if (player != null) {
            try { player.removePlayerListener(this); } catch (Exception ignored) {}
            try { player.stop();  } catch (Exception ignored) {}
            try { player.close(); } catch (Exception ignored) {}
            player    = null;
            isPlaying = false;
        }
    }

    /** JSR-135 PlayerListener — auto-advance to next track at end of media. */
    public void playerUpdate(Player p, String event, Object eventData) {
        if (!PlayerListener.END_OF_MEDIA.equals(event)) return;
        synchronized (this) {
            if (p != player) return;  // stale listener from a previous player instance
        }
        if (repeatMode == REPEAT_ONE) play(currentIndex);
        else                          next();
    }

    // -------------------------------------------------------------------------
    // State accessors
    // -------------------------------------------------------------------------

    public synchronized boolean isPlaying()  { return isPlaying; }
    public synchronized boolean isLoading()  { return isLoading; }
    public int    getCurrentIndex()          { return currentIndex; }
    public String getCurrentId()             { return safe(trackIds,     currentIndex); }
    public String getCurrentName()           { return safe(trackNames,   currentIndex); }
    public String getCurrentArtist()         { return safe(trackArtists, currentIndex); }
    public String getArtUrlTemplate()        { return artUrlTemplate != null ? artUrlTemplate : ""; }
    public int    getTrackCount()            { return trackIds != null ? trackIds.length : 0; }
    public String getTrackName(int i)        { return safe(trackNames,   i); }
    public String getTrackArtist(int i)      { return safe(trackArtists, i); }
    public String getTrackId(int i)          { return safe(trackIds,     i); }

    public long getMediaTimeMs() {
        try {
            if (player == null) return 0L;
            long t = player.getMediaTime();
            return t == Player.TIME_UNKNOWN ? 0L : t / 1000L;
        } catch (Exception e) { return 0L; }
    }

    public long getDurationMs() {
        try {
            if (player == null) return 0L;
            long d = player.getDuration();
            return d == Player.TIME_UNKNOWN ? 0L : d / 1000L;
        } catch (Exception e) { return 0L; }
    }

    // -------------------------------------------------------------------------
    // Preload scheduler
    // -------------------------------------------------------------------------

    /**
     * After a track starts playing, silently fetch+decrypt the neighbours and
     * cache them to disk.  Uses a snapshot of trackIds so queue replacement
     * mid-preload does not cause confusion.
     */
    private void schedulePreloads(int center, String[] ids) {
        if (!Settings.preloadEnabled) return;
        if (ids == null) return;
        if (center + 1 < ids.length) startPreloadThread(center + 1, ids);
        if (center - 1 >= 0)         startPreloadThread(center - 1, ids);
    }

    private void startPreloadThread(final int index, final String[] ids) {
        if (ids == null || index < 0 || index >= ids.length) return;
        final String id = ids[index];
        if (id == null || id.length() == 0) return;
        if (cachedSizes.containsKey(id)) return;

        new Thread(new Runnable() {
            public void run() {
                try {
                    if (cachedSizes.containsKey(id)) return;  // re-check after thread start
                    AMSongHelper helper = new AMSongHelper();
                    String songUrl = helper.getWebPlaybackURL(
                        id, "songs",
                        api.getDeveloperToken(), api.getUserToken());
                    byte[] data = helper.getAMDecryptedSong(
                        songUrl, null, clientIdBlob, privateKeyDer,
                        api.getDeveloperToken(), api.getUserToken());
                    if (data != null) writeToCache(id, data);
                } catch (Exception ignored) {
                    // preload failure is non-fatal — track will be fetched on demand
                }
            }
        }).start();
    }

    /**
     * Evict cached files that are no longer adjacent to the playing track.
     * Keeps at most {center-1, center, center+1} in the cache.
     */
    private static void evictStale(int center, String[] ids) {
        String keep0 = safeGet(ids, center - 1);
        String keep1 = safeGet(ids, center);
        String keep2 = safeGet(ids, center + 1);

        Vector toRemove = new Vector();
        Enumeration keys = cachedSizes.keys();
        while (keys.hasMoreElements()) {
            String id = (String) keys.nextElement();
            if (!id.equals(keep0) && !id.equals(keep1) && !id.equals(keep2))
                toRemove.addElement(id);
        }
        for (int i = 0; i < toRemove.size(); i++)
            deleteCacheFile((String) toRemove.elementAt(i));
    }

    /** Delete all cached files and remove the cache directory. */
    public static void clearCache() {
        Vector ids = new Vector();
        Enumeration keys = cachedSizes.keys();
        while (keys.hasMoreElements()) ids.addElement(keys.nextElement());
        for (int i = 0; i < ids.size(); i++)
            deleteCacheFile((String) ids.elementAt(i));

        // Remove the now-empty directory
        String dir = getCacheDir();
        if (dir != null) {
            FileConnection fc = null;
            try {
                fc = (FileConnection) Connector.open(dir, Connector.READ_WRITE);
                if (fc.exists()) {
                    Enumeration en = fc.list();
                    while (en.hasMoreElements()) {
                        String name = (String) en.nextElement();
                        if (name.endsWith(".mp4") || name.endsWith(".m4a")) {
                            FileConnection tmp = (FileConnection) Connector.open(dir + name);
                            tmp.delete();
                            tmp.close();
                        }
                    }
                }
            } catch (Exception ignored) {
                System.out.println("Cache directory error: " + dir);
                ignored.printStackTrace();
            } finally {
                if (fc != null) try { fc.close(); } catch (Exception ignored) {}
            }
            synchronized (PlaybackManager.class) {
                cacheDir         = null;
                cacheDirResolved = false;
                totalCachedBytes = 0;
                cacheQueue.removeAllElements();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Cache file I/O  (JSR-75)
    // -------------------------------------------------------------------------

    /**
     * Resolve a writable cache directory at runtime by enumerating available
     * file-system roots via FileSystemRegistry.  Preference order:
     *   1. System property  fileconn.dir.memorycard  (e.g. file:///E:/)
     *   2. First enumerated root that is not "C:/" (memory card before phone mem)
     *   3. fileconn.dir.private (app-private folder)
     *   4. "C:/" root (phone memory fallback)
     * Returns null if no root is accessible at all.
     */
    private static synchronized String getCacheDir() {
        if (cacheDirResolved) return cacheDir;
        cacheDirResolved = true;

        // 1. Prefer the memory-card root reported by the platform
        String mc = System.getProperty("fileconn.dir.memorycard");
        if (mc != null && mc.length() > 0) {
            if (!mc.endsWith("/")) mc += "/";
            String candidate = tryInitDir(mc + "wvj2me/");
            if (candidate != null) { cacheDir = candidate; return cacheDir; }
        }

        // 2. Enumerate roots — pick first non-C: root (memory card)
        String fallback = null;
        try {
            Enumeration roots = FileSystemRegistry.listRoots();
            while (roots.hasMoreElements()) {
                String root = (String) roots.nextElement(); // e.g. "E:/"
                String url  = "file:///" + root + "wvj2me/";
                if (root.toUpperCase().startsWith("C")) {
                    if (fallback == null) fallback = url;  // keep as last-resort
                    continue;
                }
                String candidate = tryInitDir(url);
                if (candidate != null) { cacheDir = candidate; return cacheDir; }
            }
        } catch (Exception ignored) {}

        // 3. App-private directory
        String priv = System.getProperty("fileconn.dir.private");
        if (priv != null && priv.length() > 0) {
            if (!priv.endsWith("/")) priv += "/";
            String candidate = tryInitDir(priv + "wvj2me/");
            if (candidate != null) { cacheDir = candidate; return cacheDir; }
        }

        // 4. C: fallback
        if (fallback != null) {
            String candidate = tryInitDir(fallback);
            if (candidate != null) { cacheDir = candidate; return cacheDir; }
        }

        return null; // no writable root found
    }

    private static String tryInitDir(String dirUrl) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(dirUrl, Connector.READ_WRITE);
            if (!fc.exists()) fc.mkdir();
            return dirUrl;
        } catch (Exception e) {
            return null;
        } finally {
            if (fc != null) try { fc.close(); } catch (Exception ignored) {}
        }
    }

    private static String cacheFilePath(String trackId) {
        String dir = getCacheDir();
        if (dir == null) return null;
        StringBuffer sb = new StringBuffer(trackId.length());
        for (int i = 0; i < trackId.length(); i++) {
            char c = trackId.charAt(i);
            if (Character.isDigit(c) || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || c == '-' || c == '_') {
                sb.append(c);
            }
        }
        if (sb.length() == 0) return null;
        return dir + sb.toString() + ".mp4";
    }

    /** Returns null on cache miss or I/O error. */
    private static byte[] readFromCache(String trackId) {
        if (!cachedSizes.containsKey(trackId)) return null;
        String path = cacheFilePath(trackId);
        if (path == null) return null;
        FileConnection fc = null;
        InputStream    in = null;
        try {
            fc = (FileConnection) Connector.open(path, Connector.READ);
            if (!fc.exists() || fc.fileSize() == 0) {
                removeSizeEntry(trackId);
                return null;
            }
            in = fc.openInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int) fc.fileSize());
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
            return baos.toByteArray();
        } catch (Exception e) {
            removeSizeEntry(trackId);
            return null;
        } finally {
            if (in != null) try { in.close();  } catch (Exception ignored) {}
            if (fc != null) try { fc.close();  } catch (Exception ignored) {}
        }
    }

    private static void writeToCache(String trackId, byte[] data) {
        if (data == null) return;
        String path = cacheFilePath(trackId);
        if (path == null) return;

        // Enforce cap — FIFO eviction: remove oldest entry until there is room
        long needed = (long) data.length;
        while (totalCachedBytes + needed > MAX_CACHE_BYTES && !cacheQueue.isEmpty()) {
            deleteCacheFile((String) cacheQueue.elementAt(0));
        }
        if (totalCachedBytes + needed > MAX_CACHE_BYTES) return; // still no room

        FileConnection fc  = null;
        OutputStream   out = null;
        try {
            fc = (FileConnection) Connector.open(path, Connector.READ_WRITE);
            if (!fc.exists()) fc.create();
            else              fc.truncate(0);
            out = fc.openOutputStream();
            out.write(data);
            out.flush();
            cachedSizes.put(trackId, new Long(needed));
            cacheQueue.addElement(trackId);
            totalCachedBytes += needed;
        } catch (Exception ignored) {
        } finally {
            if (out != null) try { out.close(); } catch (Exception ignored) {}
            if (fc  != null) try { fc.close();  } catch (Exception ignored) {}
        }
    }

    private static void deleteCacheFile(String trackId) {
        removeSizeEntry(trackId);
        String path = cacheFilePath(trackId);
        if (path == null) return;
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(path, Connector.READ_WRITE);
            if (fc.exists()) fc.delete();
        } catch (Exception ignored) {
        } finally {
            if (fc != null) try { fc.close(); } catch (Exception ignored) {}
        }
    }

    private static void removeSizeEntry(String trackId) {
        Long sz = (Long) cachedSizes.remove(trackId);
        if (sz != null) totalCachedBytes -= sz.longValue();
        if (totalCachedBytes < 0) totalCachedBytes = 0;
        cacheQueue.removeElement(trackId);
    }

    private static String safeGet(String[] arr, int i) {
        if (arr == null || i < 0 || i >= arr.length) return "";
        return arr[i] != null ? arr[i] : "";
    }

    // -------------------------------------------------------------------------
    // Listener fire helpers
    // -------------------------------------------------------------------------

    private void fireTrackChanged(int index) {
        Listener l;
        synchronized (this) { l = listener; }
        if (l != null) l.onTrackChanged(index);
    }

    private void firePlayStateChanged(boolean playing) {
        Listener l;
        synchronized (this) { l = listener; }
        if (l != null) l.onPlayStateChanged(playing);
    }

    private void fireError(String msg) {
        Listener l;
        synchronized (this) { l = listener; }
        if (l != null) l.onError(msg);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String safe(String[] arr, int i) {
        if (arr == null || i < 0 || i >= arr.length) return "";
        return arr[i] != null ? arr[i] : "";
    }
}
