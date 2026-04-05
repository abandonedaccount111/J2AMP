package com.widevine.types;

/**
 * Holds a decrypted content key extracted from a Widevine license.
 * Equivalent to the KeyContainer type in types.ts.
 */
public class KeyContainer {

    /** Key ID as lowercase hex string. */
    public final String kid;

    /** Decrypted key material as lowercase hex string. */
    public final String key;

    /** Key type (see WidevineConstants.KEY_TYPE_*). */
    public final int type;

    public KeyContainer(String kid, String key, int type) {
        this.kid  = kid;
        this.key  = key;
        this.type = type;
    }

    public String toString() {
        return "KeyContainer{kid=" + kid + ", key=" + key + ", type=" + type + "}";
    }
}
