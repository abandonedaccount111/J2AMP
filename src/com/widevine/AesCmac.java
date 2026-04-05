package com.widevine;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * AES-CMAC (RFC 4493) wrapper around Bouncy Castle's CMac implementation.
 * Ported from cmac.ts (node-widevine).
 *
 * Uses org.bouncycastle.crypto.macs.CMac with AESEngine internally.
 * Requires cldc_bccore_classes.zip on the classpath.
 */
public class AesCmac {

    private static final int MAC_SIZE = 16; // AES block size = 128 bits

    private final CMac       mac;
    private final KeyParameter keyParam;

    /**
     * @param key  AES key (16, 24, or 32 bytes).
     */
    public AesCmac(byte[] key) {
        mac      = new CMac(new AESEngine());
        keyParam = new KeyParameter(key);
        mac.init(keyParam);
    }

    /**
     * Compute AES-CMAC over message.
     * Safe to call multiple times on the same instance.
     *
     * @param message  arbitrary-length input.
     * @return 16-byte MAC tag.
     */
    public byte[] calculate(byte[] message) {
        // Re-init resets internal state and re-keys the underlying cipher.
        mac.init(keyParam);
        mac.update(message, 0, message.length);
        byte[] out = new byte[MAC_SIZE];
        mac.doFinal(out, 0);
        return out;
    }
}
