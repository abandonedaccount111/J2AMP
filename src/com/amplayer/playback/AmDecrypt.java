package com.amplayer.playback;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CTRBlockCipher;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.KeyParameter;
import com.widevine.utils.ByteUtils;

/**
 * Apple Music MP4 decryption - ported from amdecrypt.js.
 *
 * Supports CENC (AES-128-CTR) and CBCS (AES-128-CBC) schemes.
 * Parses fragmented MP4, decrypts samples, rebuilds a plain M4A.
 *
 * Main entry point: {@link #decrypt(byte[], Hashtable)}.
 */
public class AmDecrypt {

    /** Default decryption key for legacy AAC songs without per-sample keys. */
    public static final byte[] DEFAULT_SONG_DECRYPTION_KEY =
        ByteUtils.fromHex("32b8ade1769e26b1ffb8986352793fc6");

    // =========================================================================
    // Public data types
    // =========================================================================

    public static class SampleInfo {
        public byte[]   data;
        public int      duration;
        public int      descIndex;
        public byte[]   iv;
        public int[][]  subsamples; // each row: [clearBytes, encryptedBytes]

        public SampleInfo(byte[] data, int duration, int descIndex,
                          byte[] iv, int[][] subsamples) {
            this.data       = data;
            this.duration   = duration;
            this.descIndex  = descIndex;
            this.iv         = iv;
            this.subsamples = subsamples;
        }
    }

    public static class EncryptionInfo {
        public String schemeType    = "cbcs";
        public int    perSampleIvSize = 0;
        public byte[] constantIv    = new byte[0];
        public byte[] kid           = new byte[0];
    }

    public static class SongInfo {
        public Vector samples  = new Vector(); // Vector<SampleInfo>
        public byte[] moovData = new byte[0];
        public byte[] ftypData = new byte[0];
        public EncryptionInfo encryptionInfo = null;
    }

    // =========================================================================
    // Private helper types
    // =========================================================================

    private static class BoxInfo {
        int    offset;
        int    size;
        String type;
        int    headerSize;

        BoxInfo(int offset, int size, String type, int headerSize) {
            this.offset     = offset;
            this.size       = size;
            this.type       = type;
            this.headerSize = headerSize;
        }
    }

    private static class TfhdInfo {
        int     trackId        = 0;
        int     descIndex      = 0;
        int     defaultDuration;
        int     defaultSize;
        int     flags          = 0;
        boolean hasBaseDataOffset = false;
        long    baseDataOffset    = 0;

        TfhdInfo(int defaultDuration, int defaultSize) {
            this.defaultDuration = defaultDuration;
            this.defaultSize     = defaultSize;
        }
    }

    private static class TrunEntry {
        boolean hasDuration = false;
        int     duration;
        boolean hasSize     = false;
        int     size;
    }

    private static class SencEntry {
        byte[]  iv         = new byte[0];
        int[][] subsamples = new int[0][2];
    }

    // =========================================================================
    // Low-level byte helpers
    // =========================================================================

    private static long readUint32BE(byte[] buf, int off) {
        return ByteUtils.readUint32BE(buf, off);
    }

    private static int readUint16BE(byte[] buf, int off) {
        return ByteUtils.readUint16BE(buf, off);
    }

    private static int readInt32BE(byte[] buf, int off) {
        return (int) ByteUtils.readUint32BE(buf, off);
    }

    private static long readUint64BE(byte[] buf, int off) {
        long hi = readUint32BE(buf, off);
        long lo = readUint32BE(buf, off + 4);
        return (hi << 32) | lo;
    }

    private static String getBoxType(byte[] buf, int off) {
        return new String(new char[]{
            (char)(buf[off]     & 0xFF), (char)(buf[off + 1] & 0xFF),
            (char)(buf[off + 2] & 0xFF), (char)(buf[off + 3] & 0xFF)
        });
    }

    /** Search for the ASCII string {@code needle} in {@code haystack} starting at {@code from}.
     *  Returns the index of the first character, or -1. */
    private static int indexOf(byte[] haystack, String needle, int from) {
        int nlen = needle.length();
        if (nlen == 0) return from;
        int end = haystack.length - nlen;
        byte first = (byte) needle.charAt(0);
        for (int i = from; i <= end; i++) {
            if (haystack[i] == first) {
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

    private static void putUint32BE(byte[] buf, int off, long val) {
        buf[off]     = (byte)((val >>> 24) & 0xFF);
        buf[off + 1] = (byte)((val >>> 16) & 0xFF);
        buf[off + 2] = (byte)((val >>>  8) & 0xFF);
        buf[off + 3] = (byte)( val         & 0xFF);
    }

    private static void putUint64BE(byte[] buf, int off, long val) {
        putUint32BE(buf, off,     val >>> 32);
        putUint32BE(buf, off + 4, val & 0xFFFFFFFFL);
    }

    private static void putType(byte[] buf, int off, String type) {
        buf[off]     = (byte) type.charAt(0);
        buf[off + 1] = (byte) type.charAt(1);
        buf[off + 2] = (byte) type.charAt(2);
        buf[off + 3] = (byte) type.charAt(3);
    }

    private static byte[] int32BE(long n) {
        return ByteUtils.int32BE(n);
    }

    private static byte[] slice(byte[] src, int start, int end) {
        return ByteUtils.slice(src, start, end);
    }

    /** Concatenate all byte[] elements of a Vector into one array. */
    private static byte[] concatParts(Vector parts) {
        int total = 0;
        for (int i = 0; i < parts.size(); i++)
            total += ((byte[]) parts.elementAt(i)).length;
        byte[] result = new byte[total];
        int pos = 0;
        for (int i = 0; i < parts.size(); i++) {
            byte[] p = (byte[]) parts.elementAt(i);
            System.arraycopy(p, 0, result, pos, p.length);
            pos += p.length;
        }
        return result;
    }

    // =========================================================================
    // Box parsing helpers
    // =========================================================================

    /**
     * Find a child box of type {@code targetType} inside {@code data},
     * starting the scan at {@code skipHeader} bytes into {@code data}.
     */
    private static byte[] findChildBox(byte[] data, String targetType, int skipHeader) {
        int offset = skipHeader;
        while (offset + 8 <= data.length) {
            long size = readUint32BE(data, offset);
            String type = getBoxType(data, offset + 4);
            if (size < 8 || offset + size > data.length) break;
            if (type.equals(targetType))
                return slice(data, offset, (int)(offset + size));
            offset += (int) size;
        }
        return null;
    }

    private static byte[] findChildBox(byte[] data, String targetType) {
        return findChildBox(data, targetType, 8);
    }

    // =========================================================================
    // Box writing helpers
    // =========================================================================

    private static void writeBox(Vector out, String type, byte[] content) {
        byte[] header = new byte[8];
        putUint32BE(header, 0, content.length + 8);
        putType(header, 4, type);
        out.addElement(header);
        out.addElement(content);
    }

    private static void writeFullBox(Vector out, String type, int version, int flags,
                                     byte[] content) {
        byte[] header = new byte[12];
        putUint32BE(header, 0, content.length + 12);
        putType(header, 4, type);
        header[8]  = (byte)  version;
        header[9]  = (byte)((flags >> 16) & 0xFF);
        header[10] = (byte)((flags >>  8) & 0xFF);
        header[11] = (byte)( flags        & 0xFF);
        out.addElement(header);
        out.addElement(content);
    }

    // =========================================================================
    // MP4 extraction
    // =========================================================================

    /**
     * Extract song samples and metadata from a raw (encrypted) MP4 byte array.
     */
    public static SongInfo extractSongFromBuffer(byte[] rawData) {
        SongInfo songInfo = new SongInfo();

        // Collect all top-level boxes
        Vector boxes = new Vector();
        int offset = 0;
        while (offset + 8 <= rawData.length) {
            long size = readUint32BE(rawData, offset);
            String type = getBoxType(rawData, offset + 4);
            int headerSize = 8;
            if (size == 0) break;
            if (size == 1) {
                if (offset + 16 > rawData.length) break;
                size = readUint64BE(rawData, offset + 8);
                headerSize = 16;
            }
            boxes.addElement(new BoxInfo(offset, (int) size, type, headerSize));
            offset += (int) size;
        }

        for (int i = 0; i < boxes.size(); i++) {
            BoxInfo box = (BoxInfo) boxes.elementAt(i);
            if (box.type.equals("ftyp"))
                songInfo.ftypData = slice(rawData, box.offset, box.offset + box.size);
            else if (box.type.equals("moov"))
                songInfo.moovData = slice(rawData, box.offset, box.offset + box.size);
        }

        int defaultSampleDuration = 1024;
        int defaultSampleSize     = 0;
        int audioTrackId = songInfo.moovData.length > 0
            ? extractAudioTrackId(songInfo.moovData) : 1;

        if (songInfo.moovData.length > 0)
            songInfo.encryptionInfo = extractEncryptionInfo(songInfo.moovData);

        BoxInfo moofBox = null;
        for (int i = 0; i < boxes.size(); i++) {
            BoxInfo box = (BoxInfo) boxes.elementAt(i);
            if (box.type.equals("moof")) {
                moofBox = box;
            } else if (box.type.equals("mdat") && moofBox != null) {
                int ivSize = songInfo.encryptionInfo != null
                    ? songInfo.encryptionInfo.perSampleIvSize : 0;
                byte[] moofData    = slice(rawData, moofBox.offset, moofBox.offset + moofBox.size);
                byte[] mdatPayload = slice(rawData, box.offset + box.headerSize, box.offset + box.size);
                Vector samples = parseMoofMdat(
                    moofData, mdatPayload,
                    defaultSampleDuration, defaultSampleSize,
                    audioTrackId, moofBox.offset,
                    box.offset + box.headerSize, ivSize
                );
                for (int j = 0; j < samples.size(); j++)
                    songInfo.samples.addElement(samples.elementAt(j));
                moofBox = null;
            }
        }

        return songInfo;
    }

    // =========================================================================
    // moof / traf / tfhd / trun / senc parsing
    // =========================================================================

    private static Vector parseMoofMdat(
        byte[] moofData, byte[] mdatData,
        int defaultSampleDuration, int defaultSampleSize,
        int audioTrackId, int moofOffset, int mdatDataOffset,
        int perSampleIvSize
    ) {
        Vector samples = new Vector();
        int offset = 8; // skip moof box header

        while (offset + 8 <= moofData.length) {
            long size = readUint32BE(moofData, offset);
            String type = getBoxType(moofData, offset + 4);
            if (size == 0 || offset + size > moofData.length) break;

            if (type.equals("traf")) {
                TfhdInfo tfhdInfo = new TfhdInfo(defaultSampleDuration, defaultSampleSize);
                Vector   trunEntries             = new Vector();
                boolean  hasFirstTrunDataOffset  = false;
                int      firstTrunDataOffset      = 0;
                Vector   sencEntries             = new Vector();

                int trafOffset = offset + 8;
                int trafEnd    = (int)(offset + size);

                while (trafOffset + 8 <= trafEnd) {
                    long   innerSize = readUint32BE(moofData, trafOffset);
                    String innerType = getBoxType(moofData, trafOffset + 4);
                    if (innerSize == 0) break;

                    byte[] innerPayload = slice(moofData, trafOffset + 8, (int)(trafOffset + innerSize));

                    if (innerType.equals("tfhd")) {
                        parseTfhd(innerPayload, tfhdInfo);
                    } else if (innerType.equals("trun")) {
                        int[] dataOffResult = new int[]{ Integer.MIN_VALUE };
                        Vector entries = parseTrun(innerPayload, tfhdInfo, dataOffResult);
                        if (!hasFirstTrunDataOffset && dataOffResult[0] != Integer.MIN_VALUE) {
                            hasFirstTrunDataOffset = true;
                            firstTrunDataOffset    = dataOffResult[0];
                        }
                        for (int j = 0; j < entries.size(); j++)
                            trunEntries.addElement(entries.elementAt(j));
                    } else if (innerType.equals("senc")) {
                        sencEntries = parseSenc(innerPayload, perSampleIvSize);
                    }

                    trafOffset += (int) innerSize;
                }

                if (tfhdInfo.trackId != audioTrackId) {
                    offset += (int) size;
                    continue;
                }

                long base = tfhdInfo.hasBaseDataOffset ? tfhdInfo.baseDataOffset : moofOffset;
                int mdatIdx = hasFirstTrunDataOffset
                    ? (int)((base + firstTrunDataOffset) - mdatDataOffset)
                    : 0;
                int mdatReadOffset = Math.max(0, mdatIdx);

                int descIndex = tfhdInfo.descIndex;
                if (descIndex > 0) descIndex -= 1;

                for (int i = 0; i < trunEntries.size(); i++) {
                    TrunEntry entry        = (TrunEntry) trunEntries.elementAt(i);
                    int sampleSize         = entry.hasSize     ? entry.size     : tfhdInfo.defaultSize;
                    int sampleDuration     = entry.hasDuration ? entry.duration : tfhdInfo.defaultDuration;

                    if (sampleSize > 0 && mdatReadOffset + sampleSize <= mdatData.length) {
                        byte[]  sampleIv         = new byte[0];
                        int[][] sampleSubsamples  = new int[0][2];
                        if (i < sencEntries.size()) {
                            SencEntry se  = (SencEntry) sencEntries.elementAt(i);
                            sampleIv      = se.iv;
                            sampleSubsamples = se.subsamples;
                        }
                        samples.addElement(new SampleInfo(
                            slice(mdatData, mdatReadOffset, mdatReadOffset + sampleSize),
                            sampleDuration, descIndex, sampleIv, sampleSubsamples
                        ));
                        mdatReadOffset += sampleSize;
                    }
                }
            }

            offset += (int) size;
        }

        return samples;
    }

    private static void parseTfhd(byte[] data, TfhdInfo info) {
        if (data.length < 8) return;
        int flags = ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        info.flags   = flags;
        info.trackId = (int) readUint32BE(data, 4);
        int offset = 8;

        if ((flags & 0x01) != 0 && offset + 8 <= data.length) {
            info.hasBaseDataOffset = true;
            info.baseDataOffset    = readUint64BE(data, offset);
            offset += 8;
        }
        if ((flags & 0x02) != 0 && offset + 4 <= data.length) {
            info.descIndex = (int) readUint32BE(data, offset);
            offset += 4;
        }
        if ((flags & 0x08) != 0 && offset + 4 <= data.length) {
            info.defaultDuration = (int) readUint32BE(data, offset);
            offset += 4;
        }
        if ((flags & 0x10) != 0 && offset + 4 <= data.length) {
            info.defaultSize = (int) readUint32BE(data, offset);
        }
    }

    /**
     * Parse a trun box.
     * @param dataOffResult  out-param: dataOffResult[0] receives the data_offset value,
     *                       or Integer.MIN_VALUE if the flag is not set.
     */
    private static Vector parseTrun(byte[] data, TfhdInfo tfhdInfo, int[] dataOffResult) {
        Vector entries = new Vector();
        dataOffResult[0] = Integer.MIN_VALUE;
        if (data.length < 8) return entries;

        int flags       = ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        int sampleCount = (int) readUint32BE(data, 4);
        int offset = 8;

        if ((flags & 0x01) != 0 && offset + 4 <= data.length) {
            dataOffResult[0] = readInt32BE(data, offset);
            offset += 4;
        }
        if ((flags & 0x04) != 0) offset += 4; // first_sample_flags

        for (int i = 0; i < sampleCount; i++) {
            TrunEntry entry = new TrunEntry();
            if ((flags & 0x100) != 0 && offset + 4 <= data.length) {
                entry.hasDuration = true;
                entry.duration    = (int) readUint32BE(data, offset);
                offset += 4;
            }
            if ((flags & 0x200) != 0 && offset + 4 <= data.length) {
                entry.hasSize = true;
                entry.size    = (int) readUint32BE(data, offset);
                offset += 4;
            }
            if ((flags & 0x400) != 0) offset += 4; // sample_flags
            if ((flags & 0x800) != 0) offset += 4; // sample_composition_time_offset
            entries.addElement(entry);
        }
        return entries;
    }

    private static Vector parseSenc(byte[] data, int perSampleIvSize) {
        Vector entries = new Vector();
        if (data.length < 8) return entries;

        int flags       = ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        int sampleCount = (int) readUint32BE(data, 4);
        int offset = 8;

        for (int i = 0; i < sampleCount; i++) {
            SencEntry entry = new SencEntry();

            if (perSampleIvSize > 0) {
                if (offset + perSampleIvSize > data.length) break;
                entry.iv = slice(data, offset, offset + perSampleIvSize);
                offset  += perSampleIvSize;
            }

            if ((flags & 0x02) != 0) {
                if (offset + 2 > data.length) break;
                int subsampleCount = readUint16BE(data, offset);
                offset += 2;
                entry.subsamples = new int[subsampleCount][2];
                for (int j = 0; j < subsampleCount; j++) {
                    if (offset + 6 > data.length) break;
                    entry.subsamples[j][0] = readUint16BE(data, offset);
                    entry.subsamples[j][1] = (int) readUint32BE(data, offset + 2);
                    offset += 6;
                }
            }

            entries.addElement(entry);
        }
        return entries;
    }

    // =========================================================================
    // Encryption info extraction
    // =========================================================================

    public static EncryptionInfo extractEncryptionInfo(byte[] moovData) {
        byte[] trakData = findAudioTrak(moovData);
        if (trakData == null) return null;

        byte[] mdia = findChildBox(trakData, "mdia");
        if (mdia == null) return null;
        byte[] minf = findChildBox(mdia, "minf");
        if (minf == null) return null;
        byte[] stbl = findChildBox(minf, "stbl");
        if (stbl == null) return null;
        byte[] stsd = findChildBox(stbl, "stsd");
        if (stsd == null || stsd.length < 16) return null;

        int  entryOffset = 16;
        if (entryOffset + 8 > stsd.length) return null;
        long entrySize   = readUint32BE(stsd, entryOffset);
        if (entryOffset + entrySize > stsd.length) return null;
        byte[] entryData = slice(stsd, entryOffset, (int)(entryOffset + entrySize));

        byte[] sinf = findChildBox(entryData, "sinf", 36);
        if (sinf == null) return null;

        EncryptionInfo info = new EncryptionInfo();
        readSinfIntoInfo(sinf, info);
        return info;
    }

    /**
     * Returns a {@code Hashtable<Integer, EncryptionInfo>} keyed by stsd entry index, or null.
     */
    public static Hashtable extractEncryptionInfoPerStsd(byte[] moovData) {
        byte[] trakData = findAudioTrak(moovData);
        if (trakData == null) return null;

        byte[] mdia = findChildBox(trakData, "mdia");
        if (mdia == null) return null;
        byte[] minf = findChildBox(mdia, "minf");
        if (minf == null) return null;
        byte[] stbl = findChildBox(minf, "stbl");
        if (stbl == null) return null;
        byte[] stsd = findChildBox(stbl, "stsd");
        if (stsd == null || stsd.length < 16) return null;

        long entryCount = readUint32BE(stsd, 12);
        if (entryCount == 0) return null;

        Hashtable result      = new Hashtable();
        int       entryOffset = 16;

        for (int descIdx = 0; descIdx < (int) entryCount; descIdx++) {
            if (entryOffset + 8 > stsd.length) break;
            long entrySize = readUint32BE(stsd, entryOffset);
            if (entrySize < 8 || entryOffset + entrySize > stsd.length) break;

            byte[] entryData = slice(stsd, entryOffset, (int)(entryOffset + entrySize));
            byte[] sinf      = findChildBox(entryData, "sinf", 36);

            if (sinf != null) {
                EncryptionInfo info = new EncryptionInfo();
                readSinfIntoInfo(sinf, info);
                result.put(new Integer(descIdx), info);
            }

            entryOffset += (int) entrySize;
        }

        return result.size() > 0 ? result : null;
    }

    /** Shared helper: parse schm + schi/tenc out of a sinf box. */
    private static void readSinfIntoInfo(byte[] sinf, EncryptionInfo info) {
        byte[] schm = findChildBox(sinf, "schm");
        if (schm != null && schm.length >= 20)
            info.schemeType = getBoxType(schm, 12);

        byte[] schi = findChildBox(sinf, "schi");
        if (schi != null) {
            byte[] tenc = findChildBox(schi, "tenc");
            if (tenc != null && tenc.length >= 32) {
                info.perSampleIvSize = tenc[15] & 0xFF;
                info.kid             = slice(tenc, 16, 32);
                if (info.perSampleIvSize == 0 && tenc.length > 32) {
                    int constIvSize = tenc[32] & 0xFF;
                    if (tenc.length >= 33 + constIvSize)
                        info.constantIv = slice(tenc, 33, 33 + constIvSize);
                }
            }
        }
    }

    // =========================================================================
    // Track helpers
    // =========================================================================

    private static byte[] findAudioTrak(byte[] moovData) {
        int offset = 8;
        while (offset + 8 <= moovData.length) {
            long   size = readUint32BE(moovData, offset);
            String type = getBoxType(moovData, offset + 4);
            if (size < 8 || offset + size > moovData.length) break;

            if (type.equals("trak")) {
                byte[] trakData = slice(moovData, offset, (int)(offset + size));
                int    hdlrIdx  = indexOf(trakData, "hdlr");
                if (hdlrIdx > 0) {
                    int handlerOff = hdlrIdx + 4 + 4 + 4;
                    if (handlerOff + 4 <= trakData.length &&
                        getBoxType(trakData, handlerOff).equals("soun"))
                        return trakData;
                }
            }
            offset += (int) size;
        }
        return null;
    }

    private static int extractAudioTrackId(byte[] moovData) {
        int offset = 8;
        while (offset + 8 <= moovData.length) {
            long   size = readUint32BE(moovData, offset);
            String type = getBoxType(moovData, offset + 4);
            if (size < 8 || offset + size > moovData.length) break;

            if (type.equals("trak")) {
                byte[] trakData    = slice(moovData, offset, (int)(offset + size));
                int    hdlrIdx     = indexOf(trakData, "hdlr");
                if (hdlrIdx > 0) {
                    int handlerOff = hdlrIdx + 4 + 4 + 4;
                    if (handlerOff + 4 <= trakData.length &&
                        getBoxType(trakData, handlerOff).equals("soun")) {
                        int tkhdIdx = indexOf(trakData, "tkhd");
                        if (tkhdIdx > 0) {
                            int version   = trakData[tkhdIdx + 4] & 0xFF;
                            int tidOffset = (version == 0)
                                ? tkhdIdx + 4 + 4 + 4 + 4
                                : tkhdIdx + 4 + 4 + 8 + 8;
                            if (tidOffset + 4 <= trakData.length)
                                return (int) readUint32BE(trakData, tidOffset);
                        }
                    }
                }
            }
            offset += (int) size;
        }
        return 1;
    }

    // =========================================================================
    // Decryption
    // =========================================================================

    /**
     * Decrypt all samples and return the concatenated raw audio data.
     *
     * @param samples              Vector of SampleInfo
     * @param keys                 Hashtable&lt;Integer, byte[]&gt; descIndex -&gt; 16-byte AES key
     * @param encryptionInfo       global EncryptionInfo
     * @param encryptionInfoPerDesc Hashtable&lt;Integer, EncryptionInfo&gt; or null
     */
    public static byte[] decryptSamplesHex(
        Vector samples, Hashtable keys,
        EncryptionInfo encryptionInfo, Hashtable encryptionInfoPerDesc
    ) {
        boolean isCenc = "cenc".equals(encryptionInfo.schemeType);
        Vector  parts  = new Vector();

        for (int sIdx = 0; sIdx < samples.size(); sIdx++) {
            SampleInfo sample = (SampleInfo) samples.elementAt(sIdx);
            byte[]     key    = (byte[]) keys.get(new Integer(sample.descIndex));

            if (key == null) {
                parts.addElement(sample.data);
                continue;
            }

            EncryptionInfo encInfo = encryptionInfo;
            if (encryptionInfoPerDesc != null) {
                EncryptionInfo perDesc =
                    (EncryptionInfo) encryptionInfoPerDesc.get(new Integer(sample.descIndex));
                if (perDesc != null) encInfo = perDesc;
            }

            if (isCenc) {
                // ---- AES-128-CTR (CENC) ----
                byte[] iv = sample.iv;
                if (iv.length < 16) {
                    byte[] padded = new byte[16];
                    System.arraycopy(iv, 0, padded, 0, iv.length);
                    iv = padded;
                }

                CTRBlockCipher ctr = new CTRBlockCipher(new AESEngine());
                ctr.init(false, new ParametersWithIV(new KeyParameter(key), iv));

                if (sample.subsamples.length > 0) {
                    Vector plain  = new Vector();
                    int    offset = 0;
                    for (int j = 0; j < sample.subsamples.length; j++) {
                        int clearBytes = sample.subsamples[j][0];
                        int encBytes   = sample.subsamples[j][1];
                        plain.addElement(slice(sample.data, offset, offset + clearBytes));
                        offset += clearBytes;
                        byte[] dec = new byte[encBytes];
                        ctr.processBytes(sample.data, offset, encBytes, dec, 0);
                        plain.addElement(dec);
                        offset += encBytes;
                    }
                    if (offset < sample.data.length)
                        plain.addElement(slice(sample.data, offset, sample.data.length));
                    parts.addElement(concatParts(plain));
                } else {
                    byte[] dec = new byte[sample.data.length];
                    ctr.processBytes(sample.data, 0, sample.data.length, dec, 0);
                    parts.addElement(dec);
                }

            } else {
                // ---- AES-128-CBC (CBCS) ----
                byte[] iv = (sample.iv.length > 0) ? sample.iv : encInfo.constantIv;
                if (iv.length < 16) {
                    byte[] padded = new byte[16];
                    System.arraycopy(iv, 0, padded, 0, iv.length);
                    iv = padded;
                }

                if (sample.subsamples.length > 0) {
                    // Gather all encrypted regions into one buffer
                    Vector encChunks = new Vector();
                    int tmpOff = 0;
                    for (int j = 0; j < sample.subsamples.length; j++) {
                        tmpOff += sample.subsamples[j][0];
                        int encBytes = sample.subsamples[j][1];
                        if (encBytes > 0)
                            encChunks.addElement(slice(sample.data, tmpOff, tmpOff + encBytes));
                        tmpOff += encBytes;
                    }
                    byte[] encConcatBuf  = concatParts(encChunks);
                    int    totalEncLen   = encConcatBuf.length;
                    byte[] decConcat     = new byte[0];

                    if (totalEncLen > 0) {
                        int cbcLen = totalEncLen & ~0xf;
                        if (cbcLen > 0) {
                            CBCBlockCipher cbc = new CBCBlockCipher(new AESEngine());
                            cbc.init(false, new ParametersWithIV(new KeyParameter(key), iv));
                            byte[] cbcOut = new byte[cbcLen];
                            for (int b = 0; b < cbcLen; b += 16)
                                cbc.processBlock(encConcatBuf, b, cbcOut, b);
                            decConcat = cbcOut;
                        }
                        if (cbcLen < totalEncLen) {
                            byte[] tail = slice(encConcatBuf, cbcLen, totalEncLen);
                            byte[] merged = new byte[decConcat.length + tail.length];
                            System.arraycopy(decConcat, 0, merged, 0, decConcat.length);
                            System.arraycopy(tail, 0, merged, decConcat.length, tail.length);
                            decConcat = merged;
                        }
                    }

                    // Reassemble clear + decrypted
                    Vector plain  = new Vector();
                    int    offset = 0;
                    int    decOff = 0;
                    for (int j = 0; j < sample.subsamples.length; j++) {
                        int clearBytes = sample.subsamples[j][0];
                        int encBytes   = sample.subsamples[j][1];
                        plain.addElement(slice(sample.data, offset, offset + clearBytes));
                        offset += clearBytes;
                        if (encBytes > 0) {
                            plain.addElement(slice(decConcat, decOff, decOff + encBytes));
                            decOff += encBytes;
                        }
                        offset += encBytes;
                    }
                    if (offset < sample.data.length)
                        plain.addElement(slice(sample.data, offset, sample.data.length));
                    parts.addElement(concatParts(plain));

                } else {
                    int sampleLen = sample.data.length;
                    if (sampleLen == 0) {
                        parts.addElement(sample.data);
                        continue;
                    }
                    int truncated = sampleLen & ~0xf;
                    CBCBlockCipher cbc = new CBCBlockCipher(new AESEngine());
                    cbc.init(false, new ParametersWithIV(new KeyParameter(key), iv));

                    if (sampleLen % 16 == 0) {
                        byte[] dec = new byte[sampleLen];
                        for (int b = 0; b < sampleLen; b += 16)
                            cbc.processBlock(sample.data, b, dec, b);
                        parts.addElement(dec);
                    } else if (truncated > 0) {
                        byte[] dec = new byte[truncated];
                        for (int b = 0; b < truncated; b += 16)
                            cbc.processBlock(sample.data, b, dec, b);
                        byte[] tail = slice(sample.data, truncated, sampleLen);
                        byte[] full = new byte[dec.length + tail.length];
                        System.arraycopy(dec, 0, full, 0, dec.length);
                        System.arraycopy(tail, 0, full, dec.length, tail.length);
                        parts.addElement(full);
                    } else {
                        parts.addElement(sample.data);
                    }
                }
            }
        }

        return concatParts(parts);
    }

    // =========================================================================
    // MP4 writing helpers
    // =========================================================================

    private static void writeFtyp(Vector out) {
        byte[] c = new byte[24];
        c[0]='M'; c[1]='4'; c[2]='A'; c[3]=' '; // major brand
        // version = 0 at [4..7] (already zero)
        c[8]='M'; c[9]='4'; c[10]='A'; c[11]=' '; // compatible: M4A
        c[12]='m'; c[13]='p'; c[14]='4'; c[15]='2'; // compatible: mp42
        c[16]='i'; c[17]='s'; c[18]='o'; c[19]='m'; // compatible: isom
        // [20..23] = 0
        writeBox(out, "ftyp", c);
    }

    private static void writeMdat(Vector out, byte[] data) {
        writeBox(out, "mdat", data);
    }

    private static byte[] patchMvhdDuration(byte[] boxData, long duration) {
        byte[] data = new byte[boxData.length];
        System.arraycopy(boxData, 0, data, 0, boxData.length);
        if ((data[8] & 0xFF) == 0) putUint32BE(data, 24, duration);
        else                        putUint64BE(data, 32, duration);
        return data;
    }

    private static byte[] patchTkhdDuration(byte[] boxData, long duration) {
        byte[] data = new byte[boxData.length];
        System.arraycopy(boxData, 0, data, 0, boxData.length);
        data[9] = 0; data[10] = 0; data[11] = 7; // flags = 7
        if ((data[8] & 0xFF) == 0) putUint32BE(data, 28, duration);
        else                        putUint64BE(data, 36, duration);
        return data;
    }

    private static byte[] patchMdhdDuration(byte[] boxData, long duration) {
        byte[] data = new byte[boxData.length];
        System.arraycopy(boxData, 0, data, 0, boxData.length);
        if ((data[8] & 0xFF) == 0) putUint32BE(data, 24, duration);
        else                        putUint64BE(data, 32, duration);
        return data;
    }

    // =========================================================================
    // stsd extraction & cleaning
    // =========================================================================

    private static byte[] extractStsdContent(byte[] data) {
        int idx = indexOf(data, "stsd");
        if (idx < 4) return null;
        long size = readUint32BE(data, idx - 4);
        if (size < 16 || size > 10000) return null;
        byte[] raw = slice(data, idx + 4, (int)(idx - 4 + size));
        return cleanStsdContent(raw);
    }

    private static byte[] cleanStsdContent(byte[] stsdContent) {
        if (stsdContent.length < 8) return stsdContent;

        byte[] versionFlags = slice(stsdContent, 0, 4);
        long   entryCount   = readUint32BE(stsdContent, 4);
        Vector cleaned      = new Vector();
        int    offset       = 8;

        for (int i = 0; i < (int) entryCount; i++) {
            if (offset + 8 > stsdContent.length) break;
            long   entrySize = readUint32BE(stsdContent, offset);
            String entryType = getBoxType(stsdContent, offset + 4);
            if (entrySize < 8 || offset + entrySize > stsdContent.length) break;

            byte[] entryData = slice(stsdContent, offset, (int)(offset + entrySize));
            if (entryType.equals("enca") || entryType.equals("encv") ||
                entryType.equals("encs") || entryType.equals("encm"))
                cleaned.addElement(cleanEncryptedSampleEntry(entryData));
            else
                cleaned.addElement(removeSinfFromEntry(entryData));
            offset += (int) entrySize;
        }

        Vector parts = new Vector();
        parts.addElement(versionFlags);
        parts.addElement(int32BE(cleaned.size()));
        for (int i = 0; i < cleaned.size(); i++)
            parts.addElement(cleaned.elementAt(i));
        return concatParts(parts);
    }

    private static byte[] cleanEncryptedSampleEntry(byte[] entryData) {
        if (entryData.length < 36) return entryData;

        byte[] origFmt = findOriginalFormat(entryData);
        if (origFmt == null) {
            String t = getBoxType(entryData, 4);
            if      (t.equals("enca")) origFmt = new byte[]{'m','p','4','a'};
            else if (t.equals("encv")) origFmt = new byte[]{'a','v','c','1'};
            else                       origFmt = slice(entryData, 4, 8);
        }

        // Rebuild header: [size][origFmt][reserved+refIdx at bytes 8-35]
        byte[] header = new byte[36];
        System.arraycopy(entryData, 0, header, 0, 4);   // original size field (will be patched)
        System.arraycopy(origFmt,   0, header, 4, 4);   // new format
        System.arraycopy(entryData, 8, header, 8, 28);  // reserved + data ref idx

        Vector parts = new Vector();
        parts.addElement(header);

        int childOffset = 36;
        while (childOffset + 8 <= entryData.length) {
            long   childSize = readUint32BE(entryData, childOffset);
            String childType = getBoxType(entryData, childOffset + 4);
            if (childSize < 8 || childOffset + childSize > entryData.length) break;
            if (!childType.equals("sinf"))
                parts.addElement(slice(entryData, childOffset, (int)(childOffset + childSize)));
            childOffset += (int) childSize;
        }

        byte[] result = concatParts(parts);
        putUint32BE(result, 0, result.length);
        return result;
    }

    private static byte[] findOriginalFormat(byte[] entryData) {
        int  sinfIdx  = indexOf(entryData, "sinf");
        if (sinfIdx < 4) return null;
        long sinfSize = readUint32BE(entryData, sinfIdx - 4);
        if (sinfSize < 16 || sinfIdx - 4 + sinfSize > entryData.length) return null;
        byte[] sinfData = slice(entryData, sinfIdx - 4, (int)(sinfIdx - 4 + sinfSize));
        int    frmaIdx  = indexOf(sinfData, "frma");
        if (frmaIdx < 4) return null;
        long   frmaSize = readUint32BE(sinfData, frmaIdx - 4);
        if (frmaSize != 12) return null;
        return slice(sinfData, frmaIdx + 4, frmaIdx + 8);
    }

    private static byte[] removeSinfFromEntry(byte[] entryData) {
        if (entryData.length < 36 || indexOf(entryData, "sinf") < 0) return entryData;

        Vector parts = new Vector();
        parts.addElement(slice(entryData, 0, 36));
        int childOffset = 36;
        while (childOffset + 8 <= entryData.length) {
            long   childSize = readUint32BE(entryData, childOffset);
            String childType = getBoxType(entryData, childOffset + 4);
            if (childSize < 8 || childOffset + childSize > entryData.length) break;
            if (!childType.equals("sinf"))
                parts.addElement(slice(entryData, childOffset, (int)(childOffset + childSize)));
            childOffset += (int) childSize;
        }
        byte[] result = concatParts(parts);
        putUint32BE(result, 0, result.length);
        return result;
    }

    private static int extractTimescale(byte[] data) {
        int idx = indexOf(data, "mdhd");
        if (idx > 0 && idx + 24 <= data.length)
            return (int) readUint32BE(data, idx + 16);
        return 44100;
    }

    // =========================================================================
    // stts
    // =========================================================================

    private static byte[] buildStts(Vector samples) {
        Vector entries = new Vector(); // int[2]: [count, delta]
        for (int i = 0; i < samples.size(); i++) {
            SampleInfo s = (SampleInfo) samples.elementAt(i);
            if (entries.size() > 0) {
                int[] last = (int[]) entries.elementAt(entries.size() - 1);
                if (last[1] == s.duration) { last[0]++; continue; }
            }
            entries.addElement(new int[]{ 1, s.duration });
        }
        Vector parts = new Vector();
        parts.addElement(int32BE(entries.size()));
        for (int i = 0; i < entries.size(); i++) {
            int[] e = (int[]) entries.elementAt(i);
            parts.addElement(int32BE(e[0]));
            parts.addElement(int32BE(e[1]));
        }
        return concatParts(parts);
    }

    // =========================================================================
    // udta / meta
    // =========================================================================

    private static byte[] buildUdta() {
        Vector out = new Vector();

        Vector metaOut = new Vector();
        metaOut.addElement(int32BE(0)); // version + flags

        // hdlr inside meta
        Vector hdlrParts = new Vector();
        hdlrParts.addElement(int32BE(0));                                    // pre_defined
        hdlrParts.addElement(new byte[]{'m','d','i','r'});                   // handler_type
        hdlrParts.addElement(int32BE(0x6170706cL));                          // manufacturer
        hdlrParts.addElement(int32BE(0));                                    // flags
        hdlrParts.addElement(int32BE(0));                                    // flags_mask
        hdlrParts.addElement(new byte[]{ 0 });                               // name (null-term)
        writeFullBox(metaOut, "hdlr", 0, 0, concatParts(hdlrParts));

        writeBox(metaOut, "ilst", new byte[0]);

        byte[] metaContent = concatParts(metaOut);
        byte[] metaHeader  = new byte[8];
        putUint32BE(metaHeader, 0, metaContent.length + 8);
        metaHeader[4]='m'; metaHeader[5]='e'; metaHeader[6]='t'; metaHeader[7]='a';
        out.addElement(metaHeader);
        out.addElement(metaContent);

        return concatParts(out);
    }

    // =========================================================================
    // Full moov writer
    // =========================================================================

    /**
     * @param origBoxes  byte[6]{ origMvhd, origTkhd, origMdhd, origHdlr, origSmhd, origDinf }
     *                   Any element may be null to use a synthesised default.
     */
    private static byte[] buildMoov(
        Vector samples, long totalDuration, int timescale,
        byte[] stsdContent, byte[] decryptedData, byte[][] origBoxes
    ) {
        byte[] origMvhd = origBoxes[0];
        byte[] origTkhd = origBoxes[1];
        byte[] origMdhd = origBoxes[2];
        byte[] origHdlr = origBoxes[3];
        byte[] origSmhd = origBoxes[4];
        byte[] origDinf = origBoxes[5];

        Vector out = new Vector();

        // mvhd
        if (origMvhd != null) {
            out.addElement(patchMvhdDuration(origMvhd, totalDuration));
        } else {
            byte[] c = new byte[80];
            putUint32BE(c,  8, timescale);
            putUint32BE(c, 12, totalDuration);
            putUint32BE(c, 16, 0x00010000L); // rate
            c[20] = 0x01; c[21] = 0x00;     // volume
            putUint32BE(c, 32, 0x00010000L); // matrix
            putUint32BE(c, 48, 0x00010000L);
            putUint32BE(c, 64, 0x40000000L);
            putUint32BE(c, 76, 2);           // next_track_id
            writeFullBox(out, "mvhd", 0, 0, c);
        }

        // trak
        Vector trakOut = new Vector();

        // tkhd
        if (origTkhd != null) {
            trakOut.addElement(patchTkhdDuration(origTkhd, totalDuration));
        } else {
            byte[] c = new byte[84];
            putUint32BE(c,  8, 1);           // track_id
            putUint32BE(c, 20, totalDuration);
            c[32] = 0x01; c[33] = 0x00;     // volume
            putUint32BE(c, 36, 0x00010000L);
            putUint32BE(c, 52, 0x00010000L);
            putUint32BE(c, 68, 0x40000000L);
            writeFullBox(trakOut, "tkhd", 0, 7, c);
        }

        // mdia
        Vector mdiaOut = new Vector();

        // mdhd
        if (origMdhd != null) {
            mdiaOut.addElement(patchMdhdDuration(origMdhd, totalDuration));
        } else {
            byte[] c = new byte[20];
            putUint32BE(c,  8, timescale);
            putUint32BE(c, 12, totalDuration);
            c[16] = 0x55; c[17] = (byte)0xc4; // language 'und'
            writeFullBox(mdiaOut, "mdhd", 0, 0, c);
        }

        // hdlr
        if (origHdlr != null) {
            mdiaOut.addElement(origHdlr);
        } else {
            byte[] name = new byte[]{'S','o','u','n','d','H','a','n','d','l','e','r'};
            Vector hdlrParts = new Vector();
            hdlrParts.addElement(int32BE(0));
            hdlrParts.addElement(new byte[]{'s','o','u','n'});
            hdlrParts.addElement(new byte[12]);
            hdlrParts.addElement(new byte[]{ (byte) name.length });
            hdlrParts.addElement(name);
            hdlrParts.addElement(new byte[]{ 0 });
            writeFullBox(mdiaOut, "hdlr", 0, 0, concatParts(hdlrParts));
        }

        // minf
        Vector minfOut = new Vector();

        if (origSmhd != null) minfOut.addElement(origSmhd);
        else writeFullBox(minfOut, "smhd", 0, 0, new byte[4]);

        if (origDinf != null) {
            minfOut.addElement(origDinf);
        } else {
            byte[] drefEntry = new byte[12];
            putUint32BE(drefEntry, 0, 12);
            drefEntry[4]='u'; drefEntry[5]='r'; drefEntry[6]='l'; drefEntry[7]=' ';
            putUint32BE(drefEntry, 8, 1);
            byte[] drefContent = new byte[4 + 12];
            putUint32BE(drefContent, 0, 1);
            System.arraycopy(drefEntry, 0, drefContent, 4, 12);
            Vector dinfOut = new Vector();
            writeFullBox(dinfOut, "dref", 0, 0, drefContent);
            writeBox(minfOut, "dinf", concatParts(dinfOut));
        }

        // stbl
        Vector stblOut = new Vector();

        if (stsdContent != null) {
            byte[] stsdHdr = new byte[8];
            putUint32BE(stsdHdr, 0, stsdContent.length + 8);
            stsdHdr[4]='s'; stsdHdr[5]='t'; stsdHdr[6]='s'; stsdHdr[7]='d';
            stblOut.addElement(stsdHdr);
            stblOut.addElement(stsdContent);
        }

        writeFullBox(stblOut, "stts", 0, 0, buildStts(samples));

        // stsc: one chunk containing all samples
        byte[] stscContent = new byte[16];
        putUint32BE(stscContent,  0, 1);              // entry_count
        putUint32BE(stscContent,  4, 1);              // first_chunk
        putUint32BE(stscContent,  8, samples.size()); // samples_per_chunk
        putUint32BE(stscContent, 12, 1);              // sample_description_index
        writeFullBox(stblOut, "stsc", 0, 0, stscContent);

        // stsz
        Vector szParts = new Vector();
        szParts.addElement(int32BE(0));              // uniform sample_size = 0
        szParts.addElement(int32BE(samples.size()));
        for (int i = 0; i < samples.size(); i++)
            szParts.addElement(int32BE(((SampleInfo) samples.elementAt(i)).data.length));
        writeFullBox(stblOut, "stsz", 0, 0, concatParts(szParts));

        // stco placeholder — chunk offset patched later
        byte[] stcoContent = new byte[8];
        putUint32BE(stcoContent, 0, 1); // entry_count
        putUint32BE(stcoContent, 4, 0); // placeholder
        writeFullBox(stblOut, "stco", 0, 0, stcoContent);

        writeBox(minfOut, "stbl", concatParts(stblOut));
        writeBox(mdiaOut, "minf", concatParts(minfOut));
        writeBox(trakOut, "mdia", concatParts(mdiaOut));
        writeBox(trakOut, "udta", buildUdta());
        writeBox(out,     "trak", concatParts(trakOut));

        byte[] moovContent = concatParts(out);
        byte[] moovBuf     = new byte[moovContent.length + 8];
        putUint32BE(moovBuf, 0, moovBuf.length);
        moovBuf[4]='m'; moovBuf[5]='o'; moovBuf[6]='o'; moovBuf[7]='v';
        System.arraycopy(moovContent, 0, moovBuf, 8, moovContent.length);
        return moovBuf;
    }

    // =========================================================================
    // Write decrypted M4A
    // =========================================================================

    /**
     * Assemble a non-fragmented M4A byte array from decrypted audio data.
     *
     * @param songInfo      parsed song metadata
     * @param decryptedData concatenated decrypted sample data
     * @param origData      original encrypted file bytes (may be null)
     */
    public static byte[] buildDecryptedM4aBuffer(SongInfo songInfo,
                                                  byte[] decryptedData,
                                                  byte[] origData) {
        if (origData == null && songInfo.moovData.length > 0) {
            origData = new byte[songInfo.ftypData.length + songInfo.moovData.length];
            System.arraycopy(songInfo.ftypData, 0, origData, 0, songInfo.ftypData.length);
            System.arraycopy(songInfo.moovData, 0, origData, songInfo.ftypData.length,
                             songInfo.moovData.length);
        }

        byte[]   stsdContent = null;
        int      timescale   = 44100;
        byte[][] origBoxes   = new byte[6][];

        if (origData != null) {
            stsdContent = extractStsdContent(origData);
            timescale   = extractTimescale(origData);

            int moovIdx = indexOf(origData, "moov");
            if (moovIdx >= 4) {
                long   moovSize = readUint32BE(origData, moovIdx - 4);
                byte[] moovData = slice(origData, moovIdx - 4, (int)(moovIdx - 4 + moovSize));
                origBoxes[0] = findChildBox(moovData, "mvhd");
                byte[] audioTrak = findAudioTrak(moovData);
                if (audioTrak != null) {
                    origBoxes[1] = findChildBox(audioTrak, "tkhd");
                    byte[] mdia  = findChildBox(audioTrak, "mdia");
                    if (mdia != null) {
                        origBoxes[2] = findChildBox(mdia, "mdhd");
                        origBoxes[3] = findChildBox(mdia, "hdlr");
                        byte[] minf  = findChildBox(mdia, "minf");
                        if (minf != null) {
                            origBoxes[4] = findChildBox(minf, "smhd");
                            origBoxes[5] = findChildBox(minf, "dinf");
                        }
                    }
                }
            }
        }

        long totalDuration = 0;
        for (int i = 0; i < songInfo.samples.size(); i++)
            totalDuration += ((SampleInfo) songInfo.samples.elementAt(i)).duration;

        byte[] moovBuf = buildMoov(songInfo.samples, totalDuration, timescale,
                                   stsdContent, decryptedData, origBoxes);

        Vector ftypVec = new Vector();
        writeFtyp(ftypVec);
        byte[] ftypBuf = concatParts(ftypVec);

        // mdat offset = ftyp size + moov size + 8 (mdat box header)
        long mdatOffset = ftypBuf.length + moovBuf.length + 8;

        // Patch stco chunk offset: "stco" + 4 (version+flags) + 4 (entry_count) = +12
        int stcoMarker = indexOf(moovBuf, "stco");
        if (stcoMarker > 0)
            putUint32BE(moovBuf, stcoMarker + 12, mdatOffset);

        Vector mdatVec = new Vector();
        writeMdat(mdatVec, decryptedData);
        byte[] mdatBuf = concatParts(mdatVec);

        byte[] result = new byte[ftypBuf.length + moovBuf.length + mdatBuf.length];
        System.arraycopy(ftypBuf, 0, result, 0, ftypBuf.length);
        System.arraycopy(moovBuf, 0, result, ftypBuf.length, moovBuf.length);
        System.arraycopy(mdatBuf, 0, result, ftypBuf.length + moovBuf.length, mdatBuf.length);
        return result;
    }

    // =========================================================================
    // Main decrypt entry point
    // =========================================================================

    /**
     * Decrypt an encrypted MP4 byte array using a KID-to-key map.
     *
     * Key lookup strategy:
     * <ul>
     *   <li>For each stsd entry (descIndex) read its KID from tenc.</li>
     *   <li>Look up that KID (hex, case-insensitive) in keysJson.</li>
     *   <li>descIndex 0 (prefetch sample description) falls back to
     *       {@link #DEFAULT_SONG_DECRYPTION_KEY} when no match is found.</li>
     * </ul>
     *
     * @param combined  raw bytes of the encrypted MP4 file
     * @param keysJson  Hashtable&lt;String, String&gt; mapping KID hex (with or without hyphens)
     *                  to AES-128 key hex, e.g. "00000000000000000000000000000001" -&gt; "aabbcc..."
     * @return decrypted M4A file as a byte array
     */
    public static byte[] decrypt(byte[] combined, Hashtable keysJson) {
        // Normalise keys: strip hyphens, convert to lowercase
        Hashtable normKeys  = new Hashtable();
        Enumeration kidEnum = keysJson.keys();
        while (kidEnum.hasMoreElements()) {
            String kid    = (String) kidEnum.nextElement();
            String keyHex = (String) keysJson.get(kid);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < kid.length(); i++) {
                char c = kid.charAt(i);
                if (c != '-') sb.append(c);
            }
            normKeys.put(sb.toString().toLowerCase(), keyHex);
        }

        SongInfo songInfo = extractSongFromBuffer(combined);

        EncryptionInfo encInfo       = songInfo.encryptionInfo != null
            ? songInfo.encryptionInfo : new EncryptionInfo();
        Hashtable encInfoPerDesc = songInfo.moovData.length > 0
            ? extractEncryptionInfoPerStsd(songInfo.moovData) : null;

        // Build descIndex -> AES key mapping
        Hashtable keys = new Hashtable();

        if (encInfoPerDesc != null) {
            Enumeration descEnum = encInfoPerDesc.keys();
            while (descEnum.hasMoreElements()) {
                Integer        descIdxObj = (Integer) descEnum.nextElement();
                int            idx        = descIdxObj.intValue();
                EncryptionInfo info       = (EncryptionInfo) encInfoPerDesc.get(descIdxObj);
                String kidHex = (info.kid != null && info.kid.length == 16)
                    ? ByteUtils.toHex(info.kid) : null;

                if (kidHex != null && normKeys.containsKey(kidHex)) {
                    keys.put(new Integer(idx),
                             ByteUtils.fromHex((String) normKeys.get(kidHex)));
                } else if (idx == 0) {
                    keys.put(new Integer(0), DEFAULT_SONG_DECRYPTION_KEY);
                }
            }
        } else if (encInfo.kid != null && encInfo.kid.length == 16) {
            String kidHex = ByteUtils.toHex(encInfo.kid);
            if (normKeys.containsKey(kidHex))
                keys.put(new Integer(0), ByteUtils.fromHex((String) normKeys.get(kidHex)));
            else
                keys.put(new Integer(0), DEFAULT_SONG_DECRYPTION_KEY);
        } else {
            Enumeration valEnum = normKeys.elements();
            byte[] firstKey = valEnum.hasMoreElements()
                ? ByteUtils.fromHex((String) valEnum.nextElement()) : null;
            keys.put(new Integer(0),
                     firstKey != null ? firstKey : DEFAULT_SONG_DECRYPTION_KEY);
        }

        // Fill any still-missing descIndexes with the default key
        for (int i = 0; i < songInfo.samples.size(); i++) {
            Integer idxObj = new Integer(((SampleInfo) songInfo.samples.elementAt(i)).descIndex);
            if (!keys.containsKey(idxObj))
                keys.put(idxObj, DEFAULT_SONG_DECRYPTION_KEY);
        }

        byte[] decryptedData = decryptSamplesHex(
            songInfo.samples, keys, encInfo, encInfoPerDesc);
        return buildDecryptedM4aBuffer(songInfo, decryptedData, combined);
    }
}
