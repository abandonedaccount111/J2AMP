package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class RootOfTrustId implements Message {
    
  
    // RootOfTrustIdVersion
    public static final int ROOT_OF_TRUST_ID_VERSION_UNSPECIFIED = 0;
    public static final int ROOT_OF_TRUST_ID_VERSION_1 = 1;
  
    protected int version; // 1
    protected boolean _hasVersion;
    protected int keyId; // 2
    protected boolean _hasKeyId;
    protected byte[] encryptedUniqueId; // 3
    protected boolean _hasEncryptedUniqueId;
    protected byte[] uniqueIdHash; // 4
    protected boolean _hasUniqueIdHash;
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
        this._hasVersion = true;
    }
    
    public void clearVersion() {
        _hasVersion = false;
    }
    
    public boolean hasVersion() {
        return _hasVersion;
    }
    public int getKeyId() {
        return keyId;
    }
    
    public void setKeyId(int keyId) {
        this.keyId = keyId;
        this._hasKeyId = true;
    }
    
    public void clearKeyId() {
        _hasKeyId = false;
    }
    
    public boolean hasKeyId() {
        return _hasKeyId;
    }
    public byte[] getEncryptedUniqueId() {
        return encryptedUniqueId;
    }
    
    public void setEncryptedUniqueId(byte[] encryptedUniqueId) {
        this.encryptedUniqueId = encryptedUniqueId;
        this._hasEncryptedUniqueId = true;
    }
    
    public void clearEncryptedUniqueId() {
        _hasEncryptedUniqueId = false;
    }
    
    public boolean hasEncryptedUniqueId() {
        return _hasEncryptedUniqueId;
    }
    public byte[] getUniqueIdHash() {
        return uniqueIdHash;
    }
    
    public void setUniqueIdHash(byte[] uniqueIdHash) {
        this.uniqueIdHash = uniqueIdHash;
        this._hasUniqueIdHash = true;
    }
    
    public void clearUniqueIdHash() {
        _hasUniqueIdHash = false;
    }
    
    public boolean hasUniqueIdHash() {
        return _hasUniqueIdHash;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasVersion)
            out.writeInt32(1, version);
        
        if(_hasKeyId)
            out.writeUInt32(2, keyId);
        
        if(_hasEncryptedUniqueId)
            out.writeBytes(3, encryptedUniqueId);
        
        if(_hasUniqueIdHash)
            out.writeBytes(4, uniqueIdHash);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    version = in.readInt32();
                    _hasVersion = true;
                    break; }
                case 16: {
                    keyId = in.readUInt32();
                    _hasKeyId = true;
                    break; }
                case 26: {
                    encryptedUniqueId = in.readBytes();
                    _hasEncryptedUniqueId = true;
                    break; }
                case 34: {
                    uniqueIdHash = in.readBytes();
                    _hasUniqueIdHash = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static RootOfTrustId fromBytes(byte[] in) throws EncodingException {
        RootOfTrustId message = new RootOfTrustId();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



