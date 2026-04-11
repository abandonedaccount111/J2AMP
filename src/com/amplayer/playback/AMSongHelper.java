/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.amplayer.playback;

import cc.nnproject.json.*;
import com.amplayer.api.AMAPI;
import javax.microedition.io.HttpConnection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;
import com.widevine.types.KeyContainer;
import com.widevine.utils.ByteUtils;
import org.bouncycastle.util.encoders.Base64;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import java.util.Enumeration;
import com.widevine.WidevineDRM;
import com.widevine.WidevineConstants;
import com.widevine.LicenseSession;
import javax.microedition.io.Connector;
import tech.alicesworld.ModernConnectorSym93.*;
import com.amplayer.utils.IAPManager;
import com.amplayer.utils.SocketHttpConnection;
import com.amplayer.utils.IOUtils;

/**
 *
 * @author randomaccount
 */
public class AMSongHelper {
    private static final String LICENSE_URL =
        "https://play.itunes.apple.com/WebObjects/MZPlay.woa/wa/acquireWebPlaybackLicense";

    private static final String PREFETCH_KID = "00000000000000000000000000000000";
    private static final String PREFETCH_KEY = "32b8ade1769e26b1ffb8986352793fc6";

    public String getWebPlaybackURL(String id, String type, String devToken, String userToken, boolean binauralEnabled) throws Exception {
        // Get extendedAssetUrls
        try {
            AMAPI api = new AMAPI(devToken, userToken);
            String endpoint = "/v1/catalog/" + api.getStorefront() + "/songs/" + id + "?extend=extendedAssetUrls";
            String response = api.APIRequestString(endpoint, null, "GET", null, null);
            JSONObject json = JSON.getObject(response);
            JSONObject data = json.getArray("data").getObject(0);
            JSONObject attributes = data.getObject("attributes");
            JSONObject assets = attributes.getObject("extendedAssetUrls");
            String url = assets.getString("enhancedHls");

            if (url.indexOf("apple.com") >= 0) {
                url = strReplace(url, "https://", "http://");
                url = strReplace(url, "_lossless.m3u8", "_default.m3u8");
            }
            // Check if the m3u8 has wv keys
            String hlsContent = getText(url);
            if (hlsContent.indexOf("urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed") >= 0) {
                // Get lines of the hlsContent
                Vector lines = splitLines(hlsContent);
                // Find line with #EXT-X-STREAM-INF
                if (binauralEnabled) {
                    for (int i = 0; i < lines.size(); i++) {
                        String line = (String) lines.elementAt(i);
                            if (line.indexOf("gr64") >= 0 && line.indexOf("bm") >= 0) {
                                // Get the prefix of the url
                                String prefix = url.substring(0, url.lastIndexOf('/') + 1);
                                return prefix + line;
                            }
                    }   
                }
                for (int i = 0; i < lines.size(); i++) {
                    String line = (String) lines.elementAt(i);
                    if (line.indexOf("gr64") >= 0 && line.indexOf("bm") < 0) {
                            // Get the prefix of the url
                            String prefix = url.substring(0, url.lastIndexOf('/') + 1);
                            return prefix + line;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("getWebPlaybackURL error: " + e.getMessage());
            e.printStackTrace();
        }


        // If extendedAssetUrls does not have WV keys, use the old method   
        AMAPI api = new AMAPI(devToken, userToken);
        String endpoint = "WebObjects/MZPlay.woa/wa/webPlayback";
        String prefix = "https://play.itunes.apple.com/";
        // body is like {salableAdamId: "id"}
        String body = "{\"salableAdamId\":\"" + id + "\"}";
        String response = api.APIRequestString(endpoint, null, "POST", body, prefix);
        JSONObject json = JSON.getObject(response);
        System.out.println("getWebPlaybackURLResponse: " + response);
        JSONObject data = json.getArray("songList").getObject(0);
        JSONArray assets = data.getArray("assets");
        // Find object with flavor = "32:ctrp64";
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getObject(i);
            if (asset.getString("flavor").equals("32:ctrp64")) {
                return asset.getString("URL");
            }
        }
        throw new Exception("No asset with flavor 32:ctrp64 found");
    }

    public String[] getUploadedWebPlaybackURL(String id, String type, String devToken, String userToken) throws Exception {
        // System.out.println("getWebPlaybackURL: " + id + " " + type + " " + devToken + " " + userToken);
        AMAPI api = new AMAPI(devToken, userToken);
        String endpoint = "WebObjects/MZPlay.woa/wa/webPlayback";
        String prefix = "https://play.itunes.apple.com/";
        // body is like {salableAdamId: "id"}
        String body = "{\"universalLibraryId\":\"" + id + "\"}";
        String response = api.APIRequestString(endpoint, null, "POST", body, prefix);
        JSONObject json = JSON.getObject(response);
        JSONObject data = json.getArray("songList").getObject(0);
        JSONArray assets = data.getArray("assets");
        // Find object with flavor = "32:ctrp64";
        if (assets.size() > 0) {
            JSONObject asset = assets.getObject(0);
            String url = asset.getString("URL");
            String extension = asset.getObject("metadata").getString("fileExtension");
            String[] urls = new String[2];
            urls[0] = (url.indexOf("apple.com") >= 0) ? strReplace(url, "https://", "http://"): url;
            urls[1] = extension;
            return urls;
        }
        throw new Exception("No uploaded asset found");
    }

    public byte[] getAMDecryptedSong(
            String CONTENT_URL,
            String BYTE_RANGE,   // ignored; CONTENT_URL is always m3u8, full file is downloaded
            byte[] clientIdBlob,
            byte[] privateKeyDer,
            String devToken,
            String userToken
    ) {
        String tempPath = null;
        try {
            // 1. Fetch the m3u8 playlist (CONTENT_URL is always m3u8)
            String transformedUrl = replaceFirst(CONTENT_URL, "https://aod-ssl.", "http://aod.");
            String hlsContent = getText(transformedUrl);
            String baseUrl    = transformedUrl.substring(0, transformedUrl.lastIndexOf('/') + 1);

            // 2. Parse the mp4 segment URL and byte ranges from the playlist
            String  mp4Url = parseMp4Url(hlsContent, baseUrl);
            boolean isWa   = mp4Url.indexOf(".wa.mp4") >= 0;
            String  adamId = extractAdamId(mp4Url);

            int[][] ranges = parseByteRanges(hlsContent);
            if (ranges.length < 2) throw new Exception("Expected >= 2 byte ranges in playlist");
            int firstChunkLen  = ranges[0][1];
            int secondChunkLen = ranges[1][1];

            // 3. Extract Widevine URIs from the playlist
            Vector wvUris = extractWidevineUris(hlsContent);
            if (wvUris.size() == 0) throw new Exception("No Widevine EXT-X-KEY in playlist");
            String primaryUri = isWa
                ? (String) wvUris.elementAt(0)
                : (wvUris.size() > 1 ? (String) wvUris.elementAt(1)
                                      : (String) wvUris.elementAt(0));
            hlsContent = null; // free playlist string

            // 4. Fetch PSSH and acquire Widevine license before downloading the large mp4
            String initDataB64 = fetchPsshData(primaryUri);
            WidevineDRM    wv      = WidevineDRM.init(clientIdBlob, privateKeyDer,
                                                       WidevineConstants.DEVICE_TYPE_ANDROID);
            LicenseSession session = wv.createSession(Base64.decode(initDataB64),
                                                       WidevineConstants.LICENSE_TYPE_STREAMING);
            byte[] challenge    = session.generateChallenge();
            String challengeB64 = new String(Base64.encode(challenge));
            String jsonBody     = buildLicenseJson(challengeB64, primaryUri, adamId);
            String licenseJson  = postJson(LICENSE_URL, jsonBody, devToken, userToken);
            String licenseB64   = extractJsonString(licenseJson, "license");
            if (licenseB64 == null) throw new Exception("No 'license' field in response");
            Vector keys = session.parseLicense(Base64.decode(licenseB64));

            // 5. Build KID → key map
            Hashtable keysJson = new Hashtable();
            if (isWa) {
                KeyContainer kc = (KeyContainer) keys.elementAt(0);
                keysJson.put(kc.kid, kc.key);
            } else {
                KeyContainer contentKey = (KeyContainer) keys.elementAt(0);
                keysJson.put(PREFETCH_KID, PREFETCH_KEY);
                keysJson.put("00000000000000000000000000000001", contentKey.key);
            }

            // 6. Stream-download the full mp4 to a temp file instead of a byte array.
            //    This avoids holding the entire encrypted file in the JVM heap.
            tempPath = getTempFilePath(adamId);
            if (tempPath == null) {
                // No filesystem — fall back to in-memory download
                byte[] fullFile = get(mp4Url);
                return decryptInMemory(fullFile, firstChunkLen, secondChunkLen,
                                       isWa, keysJson, timescaleFrom(fullFile, firstChunkLen));
            }
            downloadToFile(mp4Url, tempPath);

            // 7. Read firstChunk and secondChunk from the temp file head.
            //    Both fit in a small header read (typically < 2 MB combined).
            int headerLen = firstChunkLen + secondChunkLen;
            byte[] header = readFileRange(tempPath, 0, headerLen);
            byte[] firstChunk  = new byte[firstChunkLen];
            byte[] secondChunk = new byte[secondChunkLen];
            System.arraycopy(header, 0,             firstChunk,  0, firstChunkLen);
            System.arraycopy(header, firstChunkLen, secondChunk, 0, secondChunkLen);
            header = null;

            // 8. Timescale + KID patching from firstChunk
            int    timescale   = readMvhdTimescale(firstChunk);
            byte[] initSegment = isWa ? firstChunk : assignSequentialKids(firstChunk);
            firstChunk = null;

            // 9. Build combined directly: alloc once, fill from initSegment then file content.
            //    Peak at this step: secondChunk (~1 MB) + initSegment (~1 MB) + combined (~8 MB)
            //    vs old peak: fullFile (8MB) + contentChunk (7MB) + combined (8MB) = 23 MB.
            long fileSize  = getFileSize(tempPath);
            int  contentLen = (int)(fileSize - firstChunkLen);
            int initLen = initSegment.length;
            byte[] combined = new byte[initLen + contentLen];
            System.arraycopy(initSegment, 0, combined, 0, initLen);
            initSegment = null; // free ~1 MB before filling content
            // Read content (file[firstChunkLen..end]) into combined[initLen..]
            fillFromFile(tempPath, firstChunkLen, combined, initLen, contentLen);

            // Delete temp file as soon as combined is built
            deleteFile(tempPath);
            tempPath = null;

            // 10. Decrypt (peak: secondChunk + combined + decrypted ≈ 17 MB)
            byte[] decrypted = AmDecrypt.decrypt(combined, keysJson);
            combined = null;
            System.gc();

            // 11. TFDT timestamp shift
            double firstTs = readFirstTfdtTimestampInSeconds(secondChunk, timescale);
            secondChunk = null;
            if (firstTs > 0) shiftTfdtTimestamps(decrypted, timescale, -firstTs);

            return decrypted;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (tempPath != null) deleteFile(tempPath);
        }
    }

    // -------------------------------------------------------------------------
    // In-memory fallback (for devices without a writable file system)
    // -------------------------------------------------------------------------

    private byte[] decryptInMemory(byte[] fullFile, int firstChunkLen, int secondChunkLen,
                                   boolean isWa, Hashtable keysJson, int timescale) {
        byte[] firstChunk   = ByteUtils.slice(fullFile, 0, firstChunkLen);
        byte[] secondChunk  = ByteUtils.slice(fullFile, firstChunkLen,
                                               firstChunkLen + secondChunkLen);
        byte[] contentChunk = ByteUtils.slice(fullFile, firstChunkLen, fullFile.length);
        fullFile = null;

        byte[] initSegment = isWa ? firstChunk : assignSequentialKids(firstChunk);
        firstChunk = null;

        byte[] combined = new byte[initSegment.length + contentChunk.length];
        System.arraycopy(initSegment,  0, combined, 0,                  initSegment.length);
        System.arraycopy(contentChunk, 0, combined, initSegment.length, contentChunk.length);
        initSegment = null; contentChunk = null;

        byte[] decrypted = AmDecrypt.decrypt(combined, keysJson);
        combined = null;
        System.gc();

        double firstTs = readFirstTfdtTimestampInSeconds(secondChunk, timescale);
        secondChunk = null;
        if (firstTs > 0) shiftTfdtTimestamps(decrypted, timescale, -firstTs);
        return decrypted;
    }

    private int timescaleFrom(byte[] fullFile, int firstChunkLen) {
        if (firstChunkLen > fullFile.length) return 48000;
        byte[] head = ByteUtils.slice(fullFile, 0, firstChunkLen);
        return readMvhdTimescale(head);
    }

    // -------------------------------------------------------------------------
    // Temp file helpers
    // -------------------------------------------------------------------------

    /** Resolve a temp path in the same cache directory PlaybackManager uses. */
    private String getTempFilePath(String adamId) {
        // Mirror PlaybackManager's cache-dir logic using a recognised system property
        String dir = IOUtils.getCacheDirectory();
        if (dir == null) return null;
        // Sanitise adamId for filename
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < adamId.length() && sb.length() < 32; i++) {
            char c = adamId.charAt(i);
            if (Character.isDigit(c) || (c>='a'&&c<='z') || (c>='A'&&c<='Z')) sb.append(c);
        }
        if (sb.length() == 0) sb.append("tmp");
        return dir + sb.toString() + "_enc.tmp";
    }

    /**
     * Stream the HTTP response body directly to a file (no in-memory buffer).
     * Writes in 32 KB chunks to keep heap usage low during download.
     */
    private void downloadToFile(String url, String filePath) throws Exception {
        HttpConnection conn = null;
        InputStream    in   = null;
        FileConnection fc   = null;
        OutputStream   out  = null;
        try {
            conn = (HttpConnection) SocketHttpConnection.open(url);
            conn.setRequestMethod(HttpConnection.GET);
            int status = conn.getResponseCode();
            if (status != HttpConnection.HTTP_OK)
                throw new Exception("HTTP " + status + " downloading mp4");
            in = conn.openInputStream();

            fc = (FileConnection) Connector.open(filePath, Connector.READ_WRITE);
            if (!fc.exists()) fc.create();
            else              fc.truncate(0);
            out = fc.openOutputStream();

            byte[] buf = new byte[32768]; // 32 KB — small footprint, good throughput
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            out.flush();
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            closeConn(conn);
            if (fc != null) try { fc.close(); } catch (Exception ignored) {}
        }
    }

    /** Read the first {@code len} bytes of a file into a new byte array. */
    private byte[] readFileRange(String filePath, int len) throws Exception {
        return readFileRange(filePath, 0, len);
    }

    /** Read {@code len} bytes starting at {@code offset} from a file into a new byte array. */
    private byte[] readFileRange(String filePath, int offset, int len) throws Exception {
        FileConnection fc = null;
        InputStream    in = null;
        try {
            fc = (FileConnection) Connector.open(filePath, Connector.READ);
            in = fc.openInputStream();
            // Skip to offset by reading and discarding
            if (offset > 0) {
                byte[] skip = new byte[Math.min(offset, 8192)];
                int remaining = offset;
                while (remaining > 0) {
                    int toSkip = Math.min(remaining, skip.length);
                    int read   = in.read(skip, 0, toSkip);
                    if (read < 0) throw new Exception("Unexpected EOF skipping to offset");
                    remaining -= read;
                }
            }
            byte[] buf = new byte[len];
            int pos = 0;
            while (pos < len) {
                int n = in.read(buf, pos, len - pos);
                if (n < 0) throw new Exception("Unexpected EOF reading file range");
                pos += n;
            }
            return buf;
        } finally {
            closeQuietly(in);
            if (fc != null) try { fc.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Read {@code len} bytes from {@code filePath} starting at {@code fileOffset}
     * directly into {@code dest[destOffset..]}, avoiding a separate allocation.
     */
    private void fillFromFile(String filePath, int fileOffset,
                              byte[] dest, int destOffset, int len) throws Exception {
        FileConnection fc = null;
        InputStream    in = null;
        try {
            fc = (FileConnection) Connector.open(filePath, Connector.READ);
            in = fc.openInputStream();
            // Skip to fileOffset
            if (fileOffset > 0) {
                byte[] skip = new byte[Math.min(fileOffset, 16384)];
                int remaining = fileOffset;
                while (remaining > 0) {
                    int toSkip = Math.min(remaining, skip.length);
                    int n      = in.read(skip, 0, toSkip);
                    if (n < 0) throw new Exception("Unexpected EOF skipping to content");
                    remaining -= n;
                }
            }
            // Read directly into dest
            int pos = destOffset;
            int end = destOffset + len;
            while (pos < end) {
                int n = in.read(dest, pos, end - pos);
                if (n < 0) break; // partial file — stop here
                pos += n;
            }
        } finally {
            closeQuietly(in);
            if (fc != null) try { fc.close(); } catch (Exception ignored) {}
        }
    }

    private long getFileSize(String filePath) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(filePath, Connector.READ);
            return fc.fileSize();
        } catch (Exception e) {
            return 0;
        } finally {
            if (fc != null) try { fc.close(); } catch (Exception ignored) {}
        }
    }

    private void deleteFile(String filePath) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(filePath, Connector.READ_WRITE);
            if (fc.exists()) fc.delete();
        } catch (Exception ignored) {
        } finally {
            if (fc != null) try { fc.close(); } catch (Exception ignored) {}
        }
    }

     // =========================================================================
    // PSSH helpers
    // =========================================================================

    /**
     * Resolve a PSSH URI to a base64-encoded PSSH box, matching fetchPsshData() in index.js.
     */
    private String fetchPsshData(String uri) throws Exception {
        if (uri.startsWith("data:")) {
            int commaIdx = uri.indexOf(',');
            if (commaIdx < 0) throw new Exception("Empty data URI for PSSH");
            String b64 = uri.substring(commaIdx + 1);
            // Short payload (<=24 chars) is a bare KID — wrap it in a full PSSH box
            if (b64.length() <= 24) {
                byte[] pssh = generateWidevinePSSH(b64);
                return new String(Base64.encode(pssh));
            }
            return b64;
        }
        return new String(Base64.encode(get(uri)));
    }

    /** Build a minimal 52-byte Widevine PSSH box with the given base64-encoded KID. */
    private static byte[] generateWidevinePSSH(String kidBase64) {
        byte[] pssh = new byte[52];
        pssh[3] = 52;                        // box size = 52
        pssh[4]='p'; pssh[5]='s'; pssh[6]='s'; pssh[7]='h';
        // version + flags = 0 at [8..11]
        // Widevine system ID at [12..27]
        byte[] sysId = { (byte)0xed,(byte)0xef,(byte)0x8b,(byte)0xa9,
                         (byte)0x79,(byte)0xd6,(byte)0x4a,(byte)0xce,
                         (byte)0xa3,(byte)0xc8,(byte)0x27,(byte)0xdc,
                         (byte)0xd5,(byte)0x1d,(byte)0x21,(byte)0xed };
        System.arraycopy(sysId, 0, pssh, 12, 16);
        pssh[31] = 20;                       // data size = 20
        // protobuf: field 1 (type=STREAMING), field 2 tag + length (KID, 16 bytes)
        pssh[32] = 0x08; pssh[33] = 0x01; pssh[34] = 0x12; pssh[35] = 0x10;
        // KID at [36..51]
        byte[] kid = Base64.decode(kidBase64);
        for (int i = 0; i < 16 && i < kid.length; i++) pssh[36 + i] = kid[i];
        return pssh;
    }

    // =========================================================================
    // KID patching
    // =========================================================================

    /**
     * Replace every KID in tenc boxes with sequential zero-padded IDs (00…00, 00…01, …).
     * Matches assignSequentialKids() in index.js.
     */
    private static byte[] assignSequentialKids(byte[] input) {
        byte[] output = new byte[input.length];
        System.arraycopy(input, 0, output, 0, input.length);
        int count = 0;
        int pos   = 0;
        while (true) {
            int tencIdx = indexOf(output, "tenc", pos);
            if (tencIdx < 0) break;
            int kidOffset = tencIdx + 12; // KID is 12 bytes after the "tenc" type field
            if (kidOffset + 16 > output.length) break;
            // Clear KID to zero
            for (int i = 0; i < 16; i++) output[kidOffset + i] = 0;
            // Write `count` as big-endian into the last bytes of the 16-byte KID
            int c = count;
            for (int i = 15; i >= 0 && c > 0; i--) {
                output[kidOffset + i] = (byte)(c & 0xFF);
                c >>>= 8;
            }
            count++;
            pos = kidOffset + 1;
        }
        return output;
    }

    // =========================================================================
    // TFDT / MVHD helpers
    // =========================================================================

    /** Read mvhd timescale; returns value in range (0, 96000], else 48000. */
    private static int readMvhdTimescale(byte[] buffer) {
        int offset = 0;
        while (offset + 8 <= buffer.length) {
            int    boxSize = (int) ByteUtils.readUint32BE(buffer, offset);
            String boxType = getType(buffer, offset + 4);
            if (boxSize < 8) break;
            if (boxType.equals("moov")) {
                int inner   = offset + 8;
                int moovEnd = offset + boxSize;
                while (inner + 8 <= moovEnd) {
                    int    subSize = (int) ByteUtils.readUint32BE(buffer, inner);
                    String subType = getType(buffer, inner + 4);
                    if (subSize < 8) break;
                    if (subType.equals("mvhd")) {
                        int version  = buffer[inner + 8] & 0xFF;
                        int tsOffset = (version == 1) ? inner + 28 : inner + 20;
                        int ts       = (int) ByteUtils.readUint32BE(buffer, tsOffset);
                        return (ts > 0 && ts <= 96000) ? ts : 48000;
                    }
                    inner += subSize;
                }
            }
            offset += boxSize;
        }
        return 48000;
    }

    /**
     * Return decode time of the first tfdt box found, in seconds.
     * Matches readFirstTfdtTimestampInSeconds() in index.js.
     */
    private static double readFirstTfdtTimestampInSeconds(byte[] mp4Bytes, int timescale) {
        int offset = 0;
        while (offset + 8 <= mp4Bytes.length) {
            int    boxSize = (int) ByteUtils.readUint32BE(mp4Bytes, offset);
            String boxType = getType(mp4Bytes, offset + 4);
            if (boxSize < 8) break;
            if (boxType.equals("moof")) {
                int inner   = offset + 8;
                int moofEnd = offset + boxSize;
                while (inner + 8 <= moofEnd) {
                    int    subSize = (int) ByteUtils.readUint32BE(mp4Bytes, inner);
                    String subType = getType(mp4Bytes, inner + 4);
                    if (subSize < 8) break;
                    if (subType.equals("traf")) {
                        int trafOff = inner + 8;
                        int trafEnd = inner + subSize;
                        while (trafOff + 8 <= trafEnd) {
                            int    sz = (int) ByteUtils.readUint32BE(mp4Bytes, trafOff);
                            String tt = getType(mp4Bytes, trafOff + 4);
                            if (sz < 8) break;
                            if (tt.equals("tfdt")) {
                                int  version = mp4Bytes[trafOff + 8] & 0xFF;
                                long time    = (version == 1)
                                    ? readUint64(mp4Bytes, trafOff + 12)
                                    : ByteUtils.readUint32BE(mp4Bytes, trafOff + 12);
                                return (double) time / (double) timescale;
                            }
                            trafOff += sz;
                        }
                    }
                    inner += subSize;
                }
            }
            offset += boxSize;
        }
        return 0.0;
    }

    /**
     * Shift all tfdt decode times by {@code shiftSeconds} (may be negative).
     * Mutates mp4Bytes in place.
     * Matches shiftTfdtTimestamps() in index.js.
     */
   public static long round(double x) {
    if (x >= 0) {
        return (long)(x + 0.5);
    } else {
        return (long)(x - 0.5);
    }
    }
    private static void shiftTfdtTimestamps(byte[] mp4Bytes, int timescale,
                                             double shiftSeconds) {
        long shiftAmount = round((double) timescale * shiftSeconds);
        int offset = 0;
        while (offset + 8 <= mp4Bytes.length) {
            int    boxSize = (int) ByteUtils.readUint32BE(mp4Bytes, offset);
            String boxType = getType(mp4Bytes, offset + 4);
            if (boxSize < 8) break;
            if (boxType.equals("moof")) {
                int inner   = offset + 8;
                int moofEnd = offset + boxSize;
                while (inner + 8 <= moofEnd) {
                    int    subSize = (int) ByteUtils.readUint32BE(mp4Bytes, inner);
                    String subType = getType(mp4Bytes, inner + 4);
                    if (subSize < 8) break;
                    if (subType.equals("traf")) {
                        int trafOff = inner + 8;
                        int trafEnd = inner + subSize;
                        while (trafOff + 8 <= trafEnd) {
                            int    sz = (int) ByteUtils.readUint32BE(mp4Bytes, trafOff);
                            String tt = getType(mp4Bytes, trafOff + 4);
                            if (sz < 8) break;
                            if (tt.equals("tfdt")) {
                                int version = mp4Bytes[trafOff + 8] & 0xFF;
                                if (version == 1) {
                                    long t    = readUint64(mp4Bytes, trafOff + 12);
                                    long newT = t + shiftAmount;
                                    if (newT < 0) newT = 0;
                                    writeUint64(mp4Bytes, trafOff + 12, newT);
                                } else {
                                    long t    = ByteUtils.readUint32BE(mp4Bytes, trafOff + 12);
                                    long newT = t + shiftAmount;
                                    if (newT < 0) newT = 0;
                                    writeUint32(mp4Bytes, trafOff + 12, newT);
                                }
                            }
                            trafOff += sz;
                        }
                    }
                    inner += subSize;
                }
            }
            offset += boxSize;
        }
    }

    // =========================================================================
    // HLS playlist parsing
    // =========================================================================

    /**
     * Find the first non-comment, non-empty line in the m3u8 — that is the mp4 segment URI.
     * Resolves relative URIs against baseUrl.
     */
    private static String parseMp4Url(String content, String baseUrl) throws Exception {
        Vector lines = splitLines(content);
        for (int i = 0; i < lines.size(); i++) {
            String line = (String) lines.elementAt(i);
            if (!line.startsWith("#") && line.length() > 0) {
                if (line.startsWith("http://") || line.startsWith("https://")) {
                    return line;
                }
                return baseUrl + line;
            }
        }
        throw new Exception("No segment URL found in m3u8");
    }

    /** Extract Adam ID from content URL. */
    private static String extractAdamId(String url) {
        int endIdx = url.indexOf("_audio");
        if (endIdx < 0) endIdx = url.indexOf(".r");
        int startIdx = url.indexOf("_A");
        if (startIdx < 0 || endIdx <= startIdx + 2) return "";
        return url.substring(startIdx + 2, endIdx);
    }

    /**
     * Parse #EXT-X-BYTERANGE entries.  Returns int[][2] where [i][0]=start, [i][1]=length.
     * Prepends a synthetic entry if the first range does not start at 0.
     */
    private static int[][] parseByteRanges(String content) {
        Vector result = new Vector();
        Vector lines  = splitLines(content);
        for (int i = 0; i < lines.size(); i++) {
            String line = (String) lines.elementAt(i);
            if (line.startsWith("#EXT-X-BYTERANGE:")) {
                String val   = line.substring("#EXT-X-BYTERANGE:".length()).trim();
                int    atIdx = val.indexOf('@');
                if (atIdx >= 0) {
                    int len   = Integer.parseInt(val.substring(0, atIdx).trim());
                    int start = Integer.parseInt(val.substring(atIdx + 1).trim());
                    result.addElement(new int[]{ start, len });
                }
            }
        }
        if (result.size() > 0) {
            int[] first = (int[]) result.elementAt(0);
            if (first[0] != 0) {
                // Prepend synthetic entry: { start: 0, length: first[0] }
                Vector v = new Vector();
                v.addElement(new int[]{ 0, first[0] });
                for (int i = 0; i < result.size(); i++) v.addElement(result.elementAt(i));
                result = v;
            }
        }
        int[][] arr = new int[result.size()][2];
        for (int i = 0; i < result.size(); i++) {
            int[] r = (int[]) result.elementAt(i);
            arr[i][0] = r[0];
            arr[i][1] = r[1];
        }
        return arr;
    }

    /**
     * Extract all Widevine #EXT-X-KEY URIs from an HLS playlist.
     * Matches extractWidevineUris() in index.js.
     */
    private static Vector extractWidevineUris(String content) {
        Vector uris  = new Vector();
        Vector lines = splitLines(content);
        for (int i = 0; i < lines.size(); i++) {
            String line = (String) lines.elementAt(i);
            if (line.startsWith("#EXT-X-KEY:") &&
                (line.indexOf("urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed") >= 0 ||
                 line.indexOf("ISO-23001-7") >= 0)) {
                int uriIdx = line.indexOf("URI=\"");
                if (uriIdx >= 0) {
                    int start = uriIdx + 5;
                    int end   = line.indexOf('"', start);
                    if (end > start) uris.addElement(line.substring(start, end));
                }
            }
        }
        return uris;
    }

    // =========================================================================
    // JSON helpers
    // =========================================================================

    private static String buildLicenseJson(String challengeB64, String uri, String adamId) {
        return "{\"challenge\":\"" + challengeB64 + "\","
             + "\"key-system\":\"com.widevine.alpha\","
             + "\"uri\":\"" + escapeJson(uri) + "\","
             + "\"adamId\":\"" + escapeJson(adamId) + "\","
             + "\"isLibrary\":false,"
             + "\"user-initiated\":true}";
    }

    /** Extract the string value of a JSON field (no nesting, no escaping in value needed). */
    private static String extractJsonString(String json, String field) {
        String search = "\"" + field + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf('"', start);
        return (end > start) ? json.substring(start, end) : null;
    }

    private static String escapeJson(String s) {
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if      (c == '"')  sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else                sb.append(c);
        }
        return sb.toString();
    }

    // =========================================================================
    // Network helpers
    // =========================================================================

    /** HTTP GET — return response body as bytes. */
    private byte[] get(String url) throws Exception {
        HttpConnection conn = null;
        InputStream    in   = null;
        try {
             if (url.startsWith("https")){
                 conn = (HttpConnection) ModernConnector.open(IAPManager.appendTo(url));
                 System.out.println("Using ModernConnector for AMP API request " + url);
             } else {
                 conn = (HttpConnection) SocketHttpConnection.open(url);
             }
            conn = (HttpConnection) SocketHttpConnection.open(url);
            IAPManager.captureFromSystem();
            conn.setRequestMethod(HttpConnection.GET);
            int status = conn.getResponseCode();
            in = conn.openInputStream();
            byte[] body = readAll(in);
            if (status != HttpConnection.HTTP_OK)
                throw new Exception("HTTP " + status + " GET " + shortenUrl(url));
            return body;
        } finally {
            closeQuietly(in);
            closeConn(conn);
        }
    }

    public InputStream getStream(String url) throws Exception {
        HttpConnection conn = null;
        InputStream    in   = null;
        try {
             if (url.startsWith("https")){
                 conn = (HttpConnection) ModernConnector.open(IAPManager.appendTo(url));
                 System.out.println("Using ModernConnector for AMP API request " + url);
             } else {
                 conn = (HttpConnection) SocketHttpConnection.open(url);
             }
            conn = (HttpConnection) SocketHttpConnection.open(url);
            IAPManager.captureFromSystem();
            conn.setRequestMethod(HttpConnection.GET);
            int status = conn.getResponseCode();
            in = conn.openInputStream();
            if (status != HttpConnection.HTTP_OK)
                throw new Exception("HTTP " + status + " GET " + shortenUrl(url));
            return in;
        } finally {
            closeConn(conn);
        }
    }

    /** HTTP GET — return response body as a String. */
    private String getText(String url) throws Exception {
        return new String(get(url), "UTF-8");
    }

    /** HTTP POST with JSON body and Apple Music auth headers; returns response as String. */
    private String postJson(String url, String body,
                             String devToken, String userToken) throws Exception {
        byte[]         bodyBytes = body.getBytes();
        HttpConnection conn      = null;
        InputStream    in        = null;
        OutputStream   out       = null;
        try {
            conn = (HttpConnection) ModernConnector.open(IAPManager.appendTo(url));
//            conn = (HttpConnection) SocketHttpConnection.open(url);
            IAPManager.captureFromSystem();
            conn.setRequestMethod(HttpConnection.POST);
            conn.setRequestProperty("Content-Type",   "application/json");
            conn.setRequestProperty("Content-Length",  String.valueOf(bodyBytes.length));
            conn.setRequestProperty("Authorization",   "Bearer " + devToken);
            conn.setRequestProperty("x-apple-music-user-token", userToken);
            conn.setRequestProperty("origin",  "https://beta.music.apple.com");
            conn.setRequestProperty("referer", "https://beta.music.apple.com");

            out = conn.openOutputStream();
            out.write(bodyBytes);
            out.flush();

            int    status  = conn.getResponseCode();
            in             = conn.openInputStream();
            String response = new String(readAll(in), "UTF-8");
            if (status != HttpConnection.HTTP_OK)
                throw new Exception("HTTP " + status + ": " + response.substring(
                    0, Math.min(120, response.length())));
            return response;
        } finally {
            closeQuietly(out);
            closeQuietly(in);
            closeConn(conn);
        }
    }

    // =========================================================================
    // Resource / IO helpers
    // =========================================================================

    private byte[] loadResource(String path) throws Exception {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) throw new Exception("Resource not found: " + path);
        try { return readAll(is); } finally { closeQuietly(is); }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    private static void closeQuietly(InputStream in) {
        if (in != null) try { in.close(); } catch (Exception ignored) {}
    }

    private static void closeQuietly(OutputStream out) {
        if (out != null) try { out.close(); } catch (Exception ignored) {}
    }

    private static void closeConn(HttpConnection conn) {
        if (conn != null) try { conn.close(); } catch (Exception ignored) {}
    }

    // =========================================================================
    // Low-level byte / string helpers
    // =========================================================================

    private static String getType(byte[] buf, int off) {
        return new String(new char[]{
            (char)(buf[off]     & 0xFF), (char)(buf[off + 1] & 0xFF),
            (char)(buf[off + 2] & 0xFF), (char)(buf[off + 3] & 0xFF)
        });
    }

    private static int indexOf(byte[] haystack, String needle, int from) {
        int nlen = needle.length();
        if (nlen == 0) return from;
        int end = haystack.length - nlen;
        for (int i = from; i <= end; i++) {
            if (haystack[i] == (byte) needle.charAt(0)) {
                boolean match = true;
                for (int j = 1; j < nlen; j++) {
                    if (haystack[i + j] != (byte) needle.charAt(j)) { match = false; break; }
                }
                if (match) return i;
            }
        }
        return -1;
    }

    private static int indexOf(byte[] haystack, String needle) {
        return indexOf(haystack, needle, 0);
    }

    private static long readUint64(byte[] buf, int off) {
        long hi = ByteUtils.readUint32BE(buf, off);
        long lo = ByteUtils.readUint32BE(buf, off + 4);
        return (hi << 32) | lo;
    }

    private static void writeUint64(byte[] buf, int off, long val) {
        writeUint32(buf, off,     val >>> 32);
        writeUint32(buf, off + 4, val & 0xFFFFFFFFL);
    }

    private static void writeUint32(byte[] buf, int off, long val) {
        buf[off]     = (byte)((val >>> 24) & 0xFF);
        buf[off + 1] = (byte)((val >>> 16) & 0xFF);
        buf[off + 2] = (byte)((val >>>  8) & 0xFF);
        buf[off + 3] = (byte)( val         & 0xFF);
    }

    private static Vector splitLines(String content) {
        Vector lines = new Vector();
        int start = 0;
        while (start < content.length()) {
            int end = content.indexOf('\n', start);
            if (end < 0) { lines.addElement(content.substring(start)); break; }
            String line = content.substring(start, end);
            if (line.length() > 0 && line.charAt(line.length() - 1) == '\r')
                line = line.substring(0, line.length() - 1);
            if (line.length() > 0) lines.addElement(line);
            start = end + 1;
        }
        return lines;
    }

    private static String replaceFirst(String str, String from, String to) {
        int idx = str.indexOf(from);
        if (idx < 0) return str;
        return str.substring(0, idx) + to + str.substring(idx + from.length());
    }

    private static String shortenUrl(String url) {
        if (url == null) return "";
        int slash = url.lastIndexOf('/');
        return slash >= 0 ? url.substring(slash + 1) : url;
    }

    private static String strReplace(String src, String from, String to) {
        int i = src.indexOf(from);
        if (i == -1) return src;
        return src.substring(0, i) + to + src.substring(i + from.length());
    }
}
