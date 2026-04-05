package com.widevine.utils;

/**
 * General-purpose byte array helpers (no java.nio dependency).
 */
public class ByteUtils {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /** Convert a byte array to a lowercase hex string. */
    public static String toHex(byte[] data) {
        if (data == null) return "";
        StringBuffer sb = new StringBuffer(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xFF;
            sb.append(HEX_CHARS[b >>> 4]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }
        return sb.toString();
    }

    /** Convert a lowercase hex string to a byte array. */
    public static byte[] fromHex(String hex) {
        if (hex == null || hex.length() == 0) return new byte[0];
        int len = hex.length();
        if ((len & 1) != 0) throw new IllegalArgumentException("Odd hex length");
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((hexVal(hex.charAt(i)) << 4) | hexVal(hex.charAt(i + 1)));
        }
        return data;
    }

    private static int hexVal(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        throw new IllegalArgumentException("Invalid hex char: " + c);
    }

    /** Concatenate two byte arrays. */
    public static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    /** Concatenate three byte arrays. */
    public static byte[] concat3(byte[] a, byte[] b, byte[] c) {
        return concat(concat(a, b), c);
    }

    /** XOR two equally-sized byte arrays (result written into dst). */
    public static void xor(byte[] dst, byte[] src) {
        for (int i = 0; i < dst.length; i++) {
            dst[i] ^= src[i];
        }
    }

    /** XOR two equally-sized byte arrays into a new array. */
    public static byte[] xorNew(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }

    /** Return a subarray [start, end). */
    public static byte[] slice(byte[] src, int start, int end) {
        byte[] result = new byte[end - start];
        System.arraycopy(src, start, result, 0, end - start);
        return result;
    }

    /** Encode a 32-bit unsigned int big-endian into 4 bytes. */
    public static byte[] int32BE(long val) {
        return new byte[] {
            (byte) ((val >>> 24) & 0xFF),
            (byte) ((val >>> 16) & 0xFF),
            (byte) ((val >>>  8) & 0xFF),
            (byte) ( val        & 0xFF)
        };
    }

    /** Read a 16-bit unsigned int big-endian from buf[off]. */
    public static int readUint16BE(byte[] buf, int off) {
        return ((buf[off] & 0xFF) << 8) | (buf[off + 1] & 0xFF);
    }

    /** Read a 32-bit unsigned int big-endian from buf[off]. */
    public static long readUint32BE(byte[] buf, int off) {
        return (((long)(buf[off    ] & 0xFF)) << 24)
             | (((long)(buf[off + 1] & 0xFF)) << 16)
             | (((long)(buf[off + 2] & 0xFF)) <<  8)
             |  ((long)(buf[off + 3] & 0xFF));
    }

    /** Deep-compare two byte arrays. */
    public static boolean equals(byte[] a, byte[] b) {
        if (a == b)           return true;
        if (a == null || b == null) return false;
        if (a.length != b.length)   return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    /** Pad data to a multiple of blockSize with PKCS#7. */
    public static byte[] pkcs7Pad(byte[] data, int blockSize) {
        int pad = blockSize - (data.length % blockSize);
        byte[] result = new byte[data.length + pad];
        System.arraycopy(data, 0, result, 0, data.length);
        for (int i = data.length; i < result.length; i++) {
            result[i] = (byte) pad;
        }
        return result;
    }

    /** Remove PKCS#7 padding. */
    public static byte[] pkcs7Unpad(byte[] data) {
        if (data.length == 0) return data;
        int pad = data[data.length - 1] & 0xFF;
        if (pad == 0 || pad > 16) return data; // no or invalid padding
        return slice(data, 0, data.length - pad);
    }

    private ByteUtils() {}
}
