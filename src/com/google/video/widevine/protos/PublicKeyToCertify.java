package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class PublicKeyToCertify implements Message {
    
  
    // KeyType
    public static final int KEY_TYPE_UNSPECIFIED = 0;
    public static final int RSA = 1;
    public static final int ECC = 2;
  
    protected byte[] publicKey; // 1
    protected boolean _hasPublicKey;
    protected int keyType; // 2
    protected boolean _hasKeyType;
    protected byte[] signature; // 3
    protected boolean _hasSignature;
    
    public byte[] getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
        this._hasPublicKey = true;
    }
    
    public void clearPublicKey() {
        _hasPublicKey = false;
    }
    
    public boolean hasPublicKey() {
        return _hasPublicKey;
    }
    public int getKeyType() {
        return keyType;
    }
    
    public void setKeyType(int keyType) {
        this.keyType = keyType;
        this._hasKeyType = true;
    }
    
    public void clearKeyType() {
        _hasKeyType = false;
    }
    
    public boolean hasKeyType() {
        return _hasKeyType;
    }
    public byte[] getSignature() {
        return signature;
    }
    
    public void setSignature(byte[] signature) {
        this.signature = signature;
        this._hasSignature = true;
    }
    
    public void clearSignature() {
        _hasSignature = false;
    }
    
    public boolean hasSignature() {
        return _hasSignature;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasPublicKey)
            out.writeBytes(1, publicKey);
        
        if(_hasKeyType)
            out.writeInt32(2, keyType);
        
        if(_hasSignature)
            out.writeBytes(3, signature);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    publicKey = in.readBytes();
                    _hasPublicKey = true;
                    break; }
                case 16: {
                    keyType = in.readInt32();
                    _hasKeyType = true;
                    break; }
                case 26: {
                    signature = in.readBytes();
                    _hasSignature = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static PublicKeyToCertify fromBytes(byte[] in) throws EncodingException {
        PublicKeyToCertify message = new PublicKeyToCertify();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



