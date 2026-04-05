package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class SignedProvisioningContext implements Message {
    
  
  
    protected byte[] provisioningContext; // 1
    protected boolean _hasProvisioningContext;
    protected byte[] signature; // 2
    protected boolean _hasSignature;
    protected int hashAlgorithm; // 3
    protected boolean _hasHashAlgorithm;
    
    public byte[] getProvisioningContext() {
        return provisioningContext;
    }
    
    public void setProvisioningContext(byte[] provisioningContext) {
        this.provisioningContext = provisioningContext;
        this._hasProvisioningContext = true;
    }
    
    public void clearProvisioningContext() {
        _hasProvisioningContext = false;
    }
    
    public boolean hasProvisioningContext() {
        return _hasProvisioningContext;
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
    public int getHashAlgorithm() {
        return hashAlgorithm;
    }
    
    public void setHashAlgorithm(int hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
        this._hasHashAlgorithm = true;
    }
    
    public void clearHashAlgorithm() {
        _hasHashAlgorithm = false;
    }
    
    public boolean hasHashAlgorithm() {
        return _hasHashAlgorithm;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasProvisioningContext)
            out.writeBytes(1, provisioningContext);
        
        if(_hasSignature)
            out.writeBytes(2, signature);
        
        if(_hasHashAlgorithm)
            out.writeInt32(3, hashAlgorithm);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    provisioningContext = in.readBytes();
                    _hasProvisioningContext = true;
                    break; }
                case 18: {
                    signature = in.readBytes();
                    _hasSignature = true;
                    break; }
                case 24: {
                    hashAlgorithm = in.readInt32();
                    _hasHashAlgorithm = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static SignedProvisioningContext fromBytes(byte[] in) throws EncodingException {
        SignedProvisioningContext message = new SignedProvisioningContext();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



