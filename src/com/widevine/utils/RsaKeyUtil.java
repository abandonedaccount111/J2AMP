package com.widevine.utils;

import java.io.IOException;
import javaf.math.BigInteger;

import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

/**
 * Minimal DER parser for RSA keys — replaces PrivateKeyFactory / PublicKeyFactory.
 *
 * Handles two private-key formats:
 *   - PKCS#8 PrivateKeyInfo (the wrapper used by OpenSSL -pkcs8, Java KeyStore, etc.)
 *   - PKCS#1 RSAPrivateKey  (the raw/traditional "SSLeay" format)
 *
 * Handles one public-key format:
 *   - SubjectPublicKeyInfo (X.509 / DER, as produced by most tools)
 *
 * All formats are strict DER (definite-length, no BER indefinite forms).
 * Only RSA keys are supported; passing any other key type will throw IOException.
 *
 * DER primitives used:
 *   0x02 INTEGER
 *   0x03 BIT STRING
 *   0x04 OCTET STRING
 *   0x05 NULL
 *   0x06 OID
 *   0x30 SEQUENCE
 */
public class RsaKeyUtil {

    // DER tags
    private static final int TAG_INTEGER    = 0x02;
    private static final int TAG_BITSTRING  = 0x03;
    private static final int TAG_OCTETSTR   = 0x04;
    private static final int TAG_NULL       = 0x05;
    private static final int TAG_OID        = 0x06;
    private static final int TAG_SEQUENCE   = 0x30;

    // OID bytes for rsaEncryption: 1.2.840.113549.1.1.1
    private static final byte[] OID_RSA = {
        0x2a, (byte)0x86, 0x48, (byte)0x86, (byte)0xf7, 0x0d, 0x01, 0x01, 0x01
    };

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parse an RSA private key from DER bytes.
     * Accepts either PKCS#8 PrivateKeyInfo or PKCS#1 RSAPrivateKey.
     *
     * @param der  DER-encoded private key bytes.
     * @return RSAPrivateCrtKeyParameters usable by Bouncy Castle engines.
     * @throws IOException if the bytes cannot be parsed as an RSA private key.
     */
    public static RSAPrivateCrtKeyParameters decodePrivateKey(byte[] der) throws IOException {
        int[] pos = { 0 };

        // Both formats start with SEQUENCE
        expectTag(der, pos, TAG_SEQUENCE);
        int seqLen = readLength(der, pos);
        int seqEnd = pos[0] + seqLen;

        // Peek at the first element: INTEGER 0 means PKCS#8 or PKCS#1
        expectTag(der, pos, TAG_INTEGER);
        int vLen = readLength(der, pos);
        byte[] version = read(der, pos, vLen);

        if (version.length == 1 && version[0] == 0) {
            // Could be PKCS#8 (next is SEQUENCE AlgorithmIdentifier)
            // or PKCS#1 (next would be INTEGER modulus — but version=0 there too)
            // Disambiguate: in PKCS#8 the next tag is SEQUENCE; in PKCS#1 it's INTEGER.
            if (pos[0] < seqEnd && (der[pos[0]] & 0xFF) == TAG_SEQUENCE) {
                return parsePkcs8(der, pos);
            } else {
                // PKCS#1: version already consumed, read remaining 8 integers
                return parsePkcs1Body(der, pos);
            }
        }
        throw new IOException("Unsupported RSA private key version: " + version[0]);
    }

    /**
     * Parse an RSA public key from SubjectPublicKeyInfo DER bytes.
     *
     * @param der  DER-encoded SubjectPublicKeyInfo.
     * @return RSAKeyParameters (public) usable by Bouncy Castle engines.
     * @throws IOException if the bytes cannot be parsed as an RSA public key.
     */
    public static RSAKeyParameters decodePublicKey(byte[] der) throws IOException {
        int[] pos = {0};

        try {
            // Try SubjectPublicKeyInfo first
            expectTag(der, pos, TAG_SEQUENCE);
            readLength(der, pos);

            // AlgorithmIdentifier
            expectTag(der, pos, TAG_SEQUENCE);
            int algLen = readLength(der, pos);
            int algEnd = pos[0] + algLen;

            expectTag(der, pos, TAG_OID);
            int oidLen = readLength(der, pos);
            byte[] oid = read(der, pos, oidLen);

            if (!ByteUtils.equals(oid, OID_RSA)) {
                throw new IOException("Not RSA OID");
            }

            pos[0] = algEnd;

            // BIT STRING
            expectTag(der, pos, TAG_BITSTRING);
            readLength(der, pos);

            int unusedBits = der[pos[0]++] & 0xFF;
            if (unusedBits != 0) {
                throw new IOException("Unexpected unused bits");
            }

            // Inner RSAPublicKey
            expectTag(der, pos, TAG_SEQUENCE);
            readLength(der, pos);

            BigInteger n = readInteger(der, pos);
            BigInteger e = readInteger(der, pos);

            return new RSAKeyParameters(false, n, e);

        } catch (Exception ignored) {
            // Reset and parse as PKCS#1
            pos[0] = 0;

            expectTag(der, pos, TAG_SEQUENCE);
            readLength(der, pos);

            BigInteger n = readInteger(der, pos);
            BigInteger e = readInteger(der, pos);

            return new RSAKeyParameters(false, n, e);
        }
    }
    // -------------------------------------------------------------------------
    // Private parsers
    // -------------------------------------------------------------------------

    /** Parse PKCS#8 PrivateKeyInfo after the version integer has been consumed. */
    private static RSAPrivateCrtKeyParameters parsePkcs8(byte[] der, int[] pos)
            throws IOException {
        // AlgorithmIdentifier SEQUENCE
        expectTag(der, pos, TAG_SEQUENCE);
        int algLen = readLength(der, pos);
        int algEnd = pos[0] + algLen;

        // OID must be rsaEncryption
        expectTag(der, pos, TAG_OID);
        int oidLen = readLength(der, pos);
        byte[] oid = read(der, pos, oidLen);
        if (!ByteUtils.equals(oid, OID_RSA)) {
            throw new IOException("PKCS#8 does not contain rsaEncryption OID");
        }
        pos[0] = algEnd; // skip NULL / params

        // OCTET STRING wrapping the RSAPrivateKey SEQUENCE
        expectTag(der, pos, TAG_OCTETSTR);
        readLength(der, pos);

        // RSAPrivateKey SEQUENCE
        expectTag(der, pos, TAG_SEQUENCE);
        readLength(der, pos);

        // version INTEGER (0)
        expectTag(der, pos, TAG_INTEGER);
        int vLen = readLength(der, pos);
        pos[0] += vLen; // skip version

        return parsePkcs1Body(der, pos);
    }

    /**
     * Parse the 8 RSA CRT parameters from a PKCS#1 RSAPrivateKey body
     * (the version INTEGER has already been consumed).
     */
    private static RSAPrivateCrtKeyParameters parsePkcs1Body(byte[] der, int[] pos)
            throws IOException {
        BigInteger n    = readInteger(der, pos); // modulus
        BigInteger e    = readInteger(der, pos); // publicExponent
        BigInteger d    = readInteger(der, pos); // privateExponent
        BigInteger p    = readInteger(der, pos); // prime1
        BigInteger q    = readInteger(der, pos); // prime2
        BigInteger dp   = readInteger(der, pos); // exponent1
        BigInteger dq   = readInteger(der, pos); // exponent2
        BigInteger qInv = readInteger(der, pos); // coefficient
        return new RSAPrivateCrtKeyParameters(n, e, d, p, q, dp, dq, qInv);
    }

    // -------------------------------------------------------------------------
    // DER low-level helpers
    // -------------------------------------------------------------------------

    private static void expectTag(byte[] der, int[] pos, int expected) throws IOException {
        int tag = der[pos[0]++] & 0xFF;
        if (tag != expected) {
            throw new IOException("DER: expected tag 0x"
                + Integer.toHexString(expected) + " but got 0x"
                + Integer.toHexString(tag) + " at offset " + (pos[0] - 1));
        }
    }

    private static int readLength(byte[] der, int[] pos) throws IOException {
        int first = der[pos[0]++] & 0xFF;
        if (first < 0x80) return first;
        int numBytes = first & 0x7F;
        if (numBytes == 0) throw new IOException("DER: indefinite length not supported");
        int len = 0;
        for (int i = 0; i < numBytes; i++) {
            len = (len << 8) | (der[pos[0]++] & 0xFF);
        }
        return len;
    }

    private static byte[] read(byte[] der, int[] pos, int len) {
        byte[] out = new byte[len];
        System.arraycopy(der, pos[0], out, 0, len);
        pos[0] += len;
        return out;
    }

    private static BigInteger readInteger(byte[] der, int[] pos) throws IOException {
        expectTag(der, pos, TAG_INTEGER);
        int len = readLength(der, pos);
        byte[] val = read(der, pos, len);
        return new BigInteger(val); // two's complement, handles leading 0x00 sign byte
    }

    private RsaKeyUtil() {}
}
