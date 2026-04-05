package com.amplayer.ui;

import com.amplayer.utils.SocketHttpConnection;
import com.amplayer.utils.URLEncoder;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.bouncycastle.crypto.digests.MD5Digest;
import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONObject;
import java.io.ByteArrayOutputStream;

/**
 * Thin Last.fm API helper (mobile auth + scrobbling).
 *
 * Authentication flow (Last.fm Mobile Auth):
 *   1. Call getMobileSession(user, password) → returns session key
 *   2. Store session key via Settings.lastFmSk
 *   3. Call updateNowPlaying() and scrobble() as needed
 *
 * All calls are blocking — run them on a background thread.
 */

public class LastFmHelper {

    private static final String API_ROOT   = "http://ws.audioscrobbler.com/2.0/";
    

    private String API_KEY    = "";
    private String API_SECRET = "";

    // -------------------------------------------------------------------------
    // Mobile auth
    // -------------------------------------------------------------------------

    public LastFmHelper() {
        try {
            byte[] resourceData = loadResource("/lastfm_token.json");
            String jsonString = new String(resourceData, "UTF-8");
            JSONObject lfmTokenFile = JSON.getObject(jsonString);
            
            if (lfmTokenFile != null) {
                API_KEY = lfmTokenFile.getString("apiKey");
                API_SECRET = lfmTokenFile.getString("sharedSecret");
            }
        } catch (Exception e) {
            // Silently handle missing or malformed configuration
        }
    }
    

    /**
     * Authenticate with username + password (Last.fm mobile auth).
     * @return session key on success
     * @throws Exception on auth failure or network error
     */
    public String getMobileSession(String username, String password) throws Exception {
        Hashtable params = new Hashtable();
        params.put("method",   "auth.getMobileSession");
        params.put("username", username);
        params.put("password", password);
        params.put("api_key",  API_KEY);
        String sig = buildApiSig(params);
        params.put("api_sig",  sig);
        params.put("format",   "json");

        String body = buildPostBody(params);
        String resp = post(API_ROOT, body);
        // Parse session.key from JSON manually (avoid dependency)
        String sk = jsonString(resp, "key");
        if (sk == null || sk.length() == 0) {
            String errMsg = jsonString(resp, "message");
            throw new Exception(errMsg != null ? errMsg : "Auth failed");
        }
        return sk;
    }

    // -------------------------------------------------------------------------
    // Scrobbling
    // -------------------------------------------------------------------------

    /**
     * Update "Now Playing" on Last.fm. Fire-and-forget (ignore errors).
     */
    public void updateNowPlaying(String sessionKey,
                                        String artist, String track, String album) {
        try {
            Hashtable params = new Hashtable();
            params.put("method",  "track.updateNowPlaying");
            params.put("artist",  artist != null ? artist : "");
            params.put("track",   track  != null ? track  : "");
            if (album != null && album.length() > 0) params.put("album", album);
            params.put("api_key", API_KEY);
            params.put("sk",      sessionKey);
            String sig = buildApiSig(params);
            params.put("api_sig", sig);
            params.put("format",  "json");
            post(API_ROOT, buildPostBody(params));
        } catch (Exception ignored) {}
    }

    /**
     * Scrobble a track.
     *
     * @param timestamp Unix timestamp (seconds) when the track started
     */
    public void scrobble(String sessionKey,
                                String artist, String track, String album,
                                long timestamp) {
        try {
            Hashtable params = new Hashtable();
            params.put("method",    "track.scrobble");
            params.put("artist[0]", artist != null ? artist : "");
            params.put("track[0]",  track  != null ? track  : "");
            if (album != null && album.length() > 0) params.put("album[0]", album);
            params.put("timestamp[0]", String.valueOf(timestamp));
            params.put("api_key",   API_KEY);
            params.put("sk",        sessionKey);
            String sig = buildApiSig(params);
            params.put("api_sig",  sig);
            params.put("format",   "json");
            post(API_ROOT, buildPostBody(params));
        } catch (Exception ignored) {}
    }

    // -------------------------------------------------------------------------
    // Signature
    // -------------------------------------------------------------------------

    /**
     * Build api_sig: alphabetically sorted param key+value pairs (no format/callback),
     * concatenated, then appended with shared secret, then MD5.
     */
    private String buildApiSig(Hashtable params) {
        // Collect & sort keys (exclude "format" and "callback")
        Vector keys = new Vector();
        Enumeration e = params.keys();
        while (e.hasMoreElements()) {
            String k = (String) e.nextElement();
            if (!"format".equals(k) && !"callback".equals(k)) keys.addElement(k);
        }
        // Bubble sort (small N, CLDC 1.1 compatible)
        for (int i = 0; i < keys.size() - 1; i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                String a = (String) keys.elementAt(i);
                String b = (String) keys.elementAt(j);
                if (a.compareTo(b) > 0) {
                    keys.setElementAt(b, i);
                    keys.setElementAt(a, j);
                }
            }
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < keys.size(); i++) {
            String k = (String) keys.elementAt(i);
            sb.append(k);
            sb.append((String) params.get(k));
        }
        sb.append(API_SECRET);
        return md5Hex(sb.toString());
    }

    // -------------------------------------------------------------------------
    // MD5 via BouncyCastle
    // -------------------------------------------------------------------------

    private static String md5Hex(String text) {
        try {
            byte[] input = text.getBytes("UTF-8");
            MD5Digest digest = new MD5Digest();
            digest.update(input, 0, input.length);
            byte[] out = new byte[16];
            digest.doFinal(out, 0);
            return hexEncode(out);
        } catch (Exception e) {
            return "";
        }
    }

    private static String hexEncode(byte[] bytes) {
        StringBuffer sb = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            if (v < 16) sb.append('0');
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // HTTP POST
    // -------------------------------------------------------------------------

    private static String post(String url, String body) throws Exception {
        SocketHttpConnection conn = SocketHttpConnection.open(url);
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            byte[] bodyBytes = body.getBytes("UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            OutputStream os = conn.openOutputStream();
            os.write(bodyBytes);
            os.flush();
            int status = conn.getResponseCode();
            InputStream is = conn.openInputStream();
            byte[] data = readAll(is);
            return new String(data, "UTF-8");
        } finally {
            try { conn.close(); } catch (Exception ignored) {}
        }
    }

    private static String buildPostBody(Hashtable params) throws Exception {
        StringBuffer sb = new StringBuffer();
        Enumeration keys = params.keys();
        boolean first = true;
        while (keys.hasMoreElements()) {
            String k = (String) keys.nextElement();
            String v = (String) params.get(k);
            if (!first) sb.append('&');
            sb.append(URLEncoder.encode(k));
            sb.append('=');
            sb.append(URLEncoder.encode(v));
            first = false;
        }
        return sb.toString();
    }

    private static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    // -------------------------------------------------------------------------
    // Minimal JSON string extractor (no full parser needed)
    // -------------------------------------------------------------------------

    /**
     * Extract the string value for a key from a flat JSON object.
     * Only handles simple string values (no nesting required here).
     */
    static String jsonString(String json, String key) {
        if (json == null) return null;
        String needle = "\"" + key + "\"";
        int ki = json.indexOf(needle);
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + needle.length());
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    private byte[] loadResource(String path) throws Exception {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) throw new Exception("Resource not found: " + path);
        try { return readAll(is); } finally { closeQuietly(is); }
    }

    private static void closeQuietly(InputStream in) {
        if (in != null) try { in.close(); } catch (Exception ignored) {}
    }
}
