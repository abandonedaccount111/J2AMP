package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class ProvisioningContextKeyData implements Message {
    
  
  
    protected byte[] encryptionKey; // 1
    protected boolean _hasEncryptionKey;
    protected byte[] encryptionIv; // 2
    protected boolean _hasEncryptionIv;
    
    public byte[] getEncryptionKey() {
        return encryptionKey;
    }
    
    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
        this._hasEncryptionKey = true;
    }
    
    public void clearEncryptionKey() {
        _hasEncryptionKey = false;
    }
    
    public boolean hasEncryptionKey() {
        return _hasEncryptionKey;
    }
    public byte[] getEncryptionIv() {
        return encryptionIv;
    }
    
    public void setEncryptionIv(byte[] encryptionIv) {
        this.encryptionIv = encryptionIv;
        this._hasEncryptionIv = true;
    }
    
    public void clearEncryptionIv() {
        _hasEncryptionIv = false;
    }
    
    public boolean hasEncryptionIv() {
        return _hasEncryptionIv;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasEncryptionKey)
            out.writeBytes(1, encryptionKey);
        
        if(_hasEncryptionIv)
            out.writeBytes(2, encryptionIv);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    encryptionKey = in.readBytes();
                    _hasEncryptionKey = true;
                    break; }
                case 18: {
                    encryptionIv = in.readBytes();
                    _hasEncryptionIv = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static ProvisioningContextKeyData fromBytes(byte[] in) throws EncodingException {
        ProvisioningContextKeyData message = new ProvisioningContextKeyData();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



